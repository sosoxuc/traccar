/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

Ext.define('Traccar.controller.Root', {
    extend: 'Ext.app.Controller',

    requires: [
        'Traccar.view.Main',
        'Traccar.view.MainMobile'
    ],

    init: function () {
        var indicator = document.createElement('div');
        indicator.className = 'state-indicator';
        document.body.appendChild(indicator);
        this.isPhone = parseInt(window.getComputedStyle(indicator).getPropertyValue('z-index'), 10) !== 0;
    },

    onLaunch: function () {
        Ext.get('spinner').remove();
        Ext.Ajax.request({
            scope: this,
            url: './api/session',
            callback: this.onSessionReturn
        });
    },

    onSessionReturn: function (options, success, response) {
        if (success) {
            var data = Ext.decode(response.responseText)
            if (data.valid) {
                Traccar.app.setServer(data.server);
                Traccar.app.setUser(data.user);
                this.loadApp();
            } else {
                location.href = 'login.html'
            }
        } else {
            Traccar.app.showError(response);
        }
    },

    loadApp: function () {
        //TODO check administration rights
        Ext.getStore('Devices').load();
        Ext.getStore('Alerts').load();
        Ext.getStore('Polygons').load();
        if (this.isPhone) {
            Ext.create('widget.mainMobile');
        } else {
            Ext.create('widget.main');
        }
        this.startWebSocket();
    },

    startWebSocket: function() {
        var wsUri;
        if (location.protocol === "https:") {
            wsUri = "wss:";
        } else {
            wsUri = "ws:";
        }
        wsUri += "//" + location.host;
        
        wsUri += location.pathname.replace("/debug.html","/") + "ws/positions";
        
        websocket = new WebSocket(wsUri);
        websocket.onopen = function(evt) {
            Ext.toast("Connection to server established")
        };
        websocket.onclose = function(evt) {
            Ext.toast("Connection to server closed")
        };
        websocket.onmessage = function(evt) {
            var i, deviceStore, positionStore, data, devices, positions, device, position;
            message = Ext.decode(evt.data);
            data = message.body;
            if (message.type == 'position') {
                devices = data.devices;
                positions = data.positions;

                deviceStore = Ext.getStore('Devices');
                positionStore = Ext.getStore('LatestPositions');
                for (i = 0; i < devices.length; i++) {
                    device = deviceStore.findRecord('id', devices[i].id, 0, false, false, true);
                    if (device) {
                        device.set({
                            status: devices[i].status,
                            lastUpdate: devices[i].lastUpdate
                        }, {
                            dirty: false
                        });
                    }
                }
    
                for (i = 0; i < positions.length; i++) {
                    position = positionStore.findRecord('deviceId', positions[i].deviceId, 0, false, false, true);
                    if (position) {
                        position.set(positions[i]);
                    } else {
                        positionStore.add(Ext.create('Traccar.model.Position', positions[i]));
                    }
                }
            } else if (message.type == 'alert') {
                alertsStore = Ext.getStore('Alerts');
                alertsStore.add(Ext.create('Traccar.model.Alert', data.message));
            }
        };
        websocket.onerror = function(evt) {
            Ext.toast("Server connection error: " +evt.data) 
        };
    }
});
