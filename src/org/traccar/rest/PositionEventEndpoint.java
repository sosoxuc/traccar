package org.traccar.rest;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.traccar.Context;
import org.traccar.database.ConnectionManager;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.web.JsonConverter;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpSession;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;

import static org.traccar.web.BaseServlet.USER_KEY;

/**
 * Created by Niko on 12/4/2015.
 */
@WebSocket
@ServerEndpoint(value = "/events/")
public class PositionEventEndpoint {
    private static final Map<Long, AsyncSession> SESSIONS = new HashMap<>();

    public static void sessionRefreshUser(long userId) {
        synchronized (SESSIONS) {
            SESSIONS.remove(userId);
        }
    }

    public static void sessionRefreshDevice(long deviceId) {
        synchronized (SESSIONS) {
            Iterator<Map.Entry<Long, AsyncSession>> iterator = SESSIONS.entrySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getValue().hasDevice(deviceId)) {
                    iterator.remove();
                }
            }
        }
    }
    public static class AsyncSession {

        public static final boolean DEBUG_ASYNC = false;

        private final Set<Long> devices = new HashSet<>();
        private final Set<Device> deviceUpdates = new HashSet<>();
        private final Set<Position> positionUpdates = new HashSet<>();
        private Session session;

        private void logEvent(String message) {
            if (DEBUG_ASYNC) {
                Log.debug("AsyncSession: " + this.hashCode() +  message);
            }
        }

        public AsyncSession(Collection<Long> devices, Session session) {
            this.devices.addAll(devices);
            this.session = session;

            Collection<Position> initialPositions = Context.getConnectionManager().getInitialState(devices);
            for (Position position : initialPositions) {
                positionUpdates.add(position);
            }

            Context.getConnectionManager().addListener(devices, dataListener);
        }

        public boolean hasDevice(long deviceId) {
            return devices.contains(deviceId);
        }

        private final ConnectionManager.UpdateListener dataListener = new ConnectionManager.UpdateListener() {
            @Override
            public void onUpdateDevice(Device device) {
                synchronized (AsyncSession.this) {
                    logEvent("onUpdateDevice deviceId: " + device.getId());

                    deviceUpdates.add(device);
                    response();
                }
            }

            @Override
            public void onUpdatePosition(Position position) {
                synchronized (AsyncSession.this) {
                    logEvent("onUpdatePosition deviceId: " + position.getDeviceId());
                    positionUpdates.add(position);
                    response();
                }
            }
        };

        public synchronized void request() {
            if (!deviceUpdates.isEmpty() || !positionUpdates.isEmpty()) {
                response();
            }
        }

        private synchronized void response() {
            JsonObjectBuilder result = Json.createObjectBuilder();
            result.add("success", true);

            if (Context.getConfig().getBoolean("web.oldAsyncFormat")) {
                result.add("data", JsonConverter.arrayToJson(positionUpdates));
            } else {
                JsonObjectBuilder data = Json.createObjectBuilder();
                data.add("devices", JsonConverter.arrayToJson(deviceUpdates));
                data.add("positions", JsonConverter.arrayToJson(positionUpdates));
                result.add("data", data.build());
            }

            try {
                session.getRemote().sendString(result.build().toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        synchronized (SESSIONS) {
            HttpSession httpSession = (HttpSession) session.getUpgradeRequest().getSession();
            Long userId = (Long) httpSession.getAttribute(USER_KEY);
            Collection<Long> devices = Context.getPermissionsManager().allowedDevices(userId);
            if (!SESSIONS.containsKey(userId)) {

                SESSIONS.put(userId, new AsyncSession(devices, session));
            }
            SESSIONS.get(userId).request();
        }
    }

    @OnWebSocketMessage
    public void onWebSocketText(String message) {
        throw new RuntimeException("not implemented");
    }

    @OnWebSocketClose
    public void onWebSocketClose(Session session, int status, String reason) {
        synchronized (SESSIONS) {
            HttpSession httpSession = (HttpSession) session.getUpgradeRequest().getSession();
            Long userId = (Long) httpSession.getAttribute(USER_KEY);
            Collection<Long> devices = Context.getPermissionsManager().allowedDevices(userId);

        }

    }

    @OnWebSocketError
    public void onWebSocketError(Throwable cause) {
        cause.printStackTrace(System.err);
    }

}
