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
package org.traccar;

import java.net.SocketAddress;
import java.util.*;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.traccar.model.Position;

public abstract class ExtendedObjectDecoder implements ChannelUpstreamHandler {

    @Override
    public void handleUpstream(
            ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
        if (!(evt instanceof MessageEvent)) {
            ctx.sendUpstream(evt);
            return;
        }

        MessageEvent e = (MessageEvent) evt;
        Object originalMessage = e.getMessage();
        Object decodedMessage = decode(e.getChannel(), e.getRemoteAddress(), originalMessage);
        onMessageEvent(e.getChannel(), e.getRemoteAddress(), originalMessage); // call after decode
        if (originalMessage == decodedMessage) {
            ctx.sendUpstream(evt);
        } else if (decodedMessage != null) {
            if (decodedMessage instanceof Collection) {
                List messages = new ArrayList((Collection) decodedMessage);
                Collections.sort(messages, new Comparator<Object>() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        if (o1 instanceof Position && o2 instanceof Position) {
                            Position p1 = (Position)o1;
                            Position p2 = (Position)o2;

                            return p1.getDeviceTime().compareTo(p2.getDeviceTime());
                        }
                        return 0;
                    }
                });

                Iterator iterator = messages.iterator();
                while (iterator.hasNext()) {
                    Position p = (Position)iterator.next();
                    if (iterator.hasNext()) {
                        p.setLatest(false);
                    }
                    Channels.fireMessageReceived(ctx, p, e.getRemoteAddress());
                }

            } else {
                Channels.fireMessageReceived(ctx, decodedMessage, e.getRemoteAddress());
            }
        }
    }

    protected void onMessageEvent(Channel channel, SocketAddress remoteAddress, Object msg) {
    }

    protected abstract Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception;

}
