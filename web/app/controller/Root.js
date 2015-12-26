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
        'Traccar.view.Login',
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
        Ext.Ajax.request({
            scope: this,
            url: '/api/server',
            callback: this.onServerReturn
        });
    },

    onServerReturn: function (options, success, response) {
        Ext.get('spinner').remove();
        if (success) {
            Traccar.app.setServer(Ext.decode(response.responseText));
            Ext.Ajax.request({
                scope: this,
                url: '/api/session',
                callback: this.onSessionReturn
            });
        } else {
            Traccar.app.showError(response);
        }
    },

    onSessionReturn: function (options, success, response) {
        if (success) {
            Traccar.app.setUser(Ext.decode(response.responseText));
            this.loadApp();
        } else {
            this.login = Ext.create('widget.login', {
                listeners: {
                    scope: this,
                    login: this.onLogin
                }
            });
            this.login.show();
        }
    },

    onLogin: function () {
        this.login.close();
        this.loadApp();
    },

    loadApp: function () {
        Ext.getStore('Devices').load();
        Ext.getStore('Polygons').load();
        Ext.get('attribution').remove();
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
        wsUri += location.pathname + "api/ws/positions/";
        
        websocket = new WebSocket(wsUri);
        websocket.onopen = function(evt) {
            Ext.toast("Connection to server established")
        };
        websocket.onclose = function(evt) {
            Ext.toast("Connection to server closed")
        };
        websocket.onmessage = function(evt) {
            var i, deviceStore, positionStore, data, devices, positions, device, position;
            deviceStore = Ext.getStore('Devices');
            positionStore = Ext.getStore('LatestPositions');
            data = Ext.decode(evt.data).data;
            devices = data.devices;
            positions = data.positions;

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
        };
        websocket.onerror = function(evt) {
            Ext.toast("Server connection error: " +evt.data) 
        };
    },

    asyncUpdate: function (first) {
        var socket = new WebSocket('ws://' + window.location.host + '/api/socket');

        socket.onmessage = function (event) {
            var i, store, data, array, entity;

            data = Ext.decode(event.data);

            if (data.devices) {
                array = data.devices;
                store = Ext.getStore('Devices');
                for (i = 0; i < array.length; i++) {
                    entity = store.findRecord('id', array[i].id, 0, false, false, true);
                    if (entity) {
                        entity.set({
                            status: array[i].status,
                            lastUpdate: array[i].lastUpdate
                        }, {
                            dirty: false
                        });
                    }
                }
            }

            if (data.positions) {
                array = data.positions;
                store = Ext.getStore('LatestPositions');
                for (i = 0; i < array.length; i++) {
                    entity = store.findRecord('deviceId', array[i].deviceId, 0, false, false, true);
                    if (entity) {
                        entity.set(array[i]);
                    } else {
                        store.add(Ext.create('Traccar.model.Position', array[i]));
                    }
                }
            }
        };
    }
});
