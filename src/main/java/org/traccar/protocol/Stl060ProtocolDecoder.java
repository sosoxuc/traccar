/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.protocol;

import java.net.SocketAddress;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class Stl060ProtocolDecoder extends BaseProtocolDecoder {

    public Stl060ProtocolDecoder(Stl060Protocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .any()
            .text("$1,")
            .number("(d+),")                     // imei
            .text("D001,")                       // type
            .expression("[^,]*,")                // vehicle
            .number("(dd)/(dd)/(dd),")           // date
            .number("(dd):(dd):(dd),")           // time
            .number("(dd)(dd).?(d+)([NS]),")     // latitude
            .number("(ddd)(dd).?(d+)([EW]),")    // longitude
            .number("(d+.?d*),")                 // speed
            .number("(d+.?d*),")                 // course
            .groupBegin()
            .number("(d+),")                     // odometer
            .number("(d+),")                     // Ignition
            .number("(d+),")                     // di1
            .number("(d+),")                     // di2
            .number("(d+),")                     // fuel
            .or()
            .expression("([01]),")               // charging
            .expression("([01]),")               // ignition
            .expression("0,0,")                  // reserved
            .number("(d+),")                     // di
            .expression("([^,]+),")              // rfid
            .number("(d+),")                     // odometer
            .number("(d+),")                     // temperature
            .number("(d+),")                     // fuel
            .expression("([01]),")               // accelerometer
            .expression("([01]),")               // do1
            .expression("([01]),")               // do2
            .groupEnd()
            .expression("([AV])")                // validity
            .any()
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (!identify(parser.next(), channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        DateBuilder dateBuilder = new DateBuilder()
                .setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt())
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.setLatitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_MIN_HEM));
        position.setLongitude(parser.nextCoordinate(Parser.CoordinateFormat.DEG_MIN_MIN_HEM));
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());

        // Old format
        if (parser.hasNext(5)) {
            position.set(Event.KEY_ODOMETER, parser.nextInt());
            position.set(Event.KEY_IGNITION, parser.nextInt());
            position.set(Event.KEY_INPUT, parser.nextInt() + parser.nextInt() << 1);
            position.set(Event.KEY_FUEL, parser.nextInt());
        }

        // New format
        if (parser.hasNext(10)) {
            position.set(Event.KEY_CHARGE, parser.nextInt() == 1);
            position.set(Event.KEY_IGNITION, parser.nextInt());
            position.set(Event.KEY_INPUT, parser.nextInt());
            position.set(Event.KEY_RFID, parser.next());
            position.set(Event.KEY_ODOMETER, parser.nextInt());
            position.set(Event.PREFIX_TEMP + 1, parser.nextInt());
            position.set(Event.KEY_FUEL, parser.nextInt());
            position.set("accel", parser.nextInt() == 1);
            position.set(Event.KEY_OUTPUT, parser.nextInt() + parser.nextInt() << 1);
        }

        position.setValid(parser.next().equals("A"));

        return position;
    }

}
