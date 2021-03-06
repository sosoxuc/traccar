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

Ext.define('Traccar.view.Login', {
    extend: 'Ext.window.Window',
    alias: 'widget.login',
    autoShow: true,

    requires: [
        'Traccar.view.LoginController'
    ],

    controller: 'login',

    title: Strings.loginTitle,
    closable: false,
    bodyPadding: Traccar.Style.panelPadding,
    resizable: false,
    modal: false,

    items: {
        xtype: 'form',
        reference: 'form',

        autoEl: {
            tag: 'form',
            method: 'GET',
            action: 'blank.html',
            target: 'submitTarget'
        },

        items: [{
            xtype: 'combobox',
            name: 'language',
            fieldLabel: Strings.loginLanguage,
            store: 'Languages',
            displayField: 'name',
            valueField: 'code',
            submitValue: false,
            reference: 'languageField',
            listeners: {
                select: 'onSelectLanguage'
            }
        }, {
            xtype: 'textfield',
            name: 'email',
            fieldLabel: Strings.userEmail,
            allowBlank: false,
            enableKeyEvents: true,
            listeners: {
                specialKey: 'onSpecialKey',
                afterrender: 'onAfterRender'
            },
            inputAttrTpl: ['autocomplete="on"']
        }, {
            xtype: 'textfield',
            name: 'password',
            fieldLabel: Strings.userPassword,
            inputType: 'password',
            allowBlank: false,
            enableKeyEvents: true,
            listeners: {
                specialKey: 'onSpecialKey'
            },
            inputAttrTpl: ['autocomplete="on"']
        }, {
            xtype: 'component',
            html: '<iframe id="submitTarget" name="submitTarget" style="display:none"></iframe>'
        }, {
            xtype: 'component',
            html: '<input type="submit" id="submitButton" style="display:none">'
        }]
    },

    buttons: [{
        xtype: 'container',
        bodyCls: 'x-toolbar-footer',
        layout: {
            type: 'vbox',
            align: 'left'
        },
        items: [{
            xtype: 'component',
            autoEl: {
                tag: 'a',
                html: Strings.loginRegister,
                href: '',
                onclick: 'event.preventDefault();Ext.create(\'Traccar.view.Register\').show();',
                style: 'margin: 5px;'
            }
        }, {
            xtype: 'component',
            autoEl: {
                tag: 'a',
                html: 'Android app',
                href: './downloads/traccar.apk',
                target: '_blank',
                style: 'margin: 5px; background-color:inherit;'
            }
        }]
    }
              
,'->', {
        text: Strings.loginLogin,
        handler: 'onLoginClick'
    }]
});
