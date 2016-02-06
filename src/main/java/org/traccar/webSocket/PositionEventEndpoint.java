package org.traccar.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.traccar.Context;
import org.traccar.database.ConnectionManager;
import org.traccar.geofence.Alert;
import org.traccar.geofence.Message;
import org.traccar.geofence.Notification;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.rest.SessionResource;
import org.traccar.rest.utils.DateTimeFormatter;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Niko on 12/4/2015.
 */
@ServerEndpoint(value = "/ws/positions", configurator = SessionConfigurator.class)
public class PositionEventEndpoint {
    private static final Map<Long, Map<Session, AsyncSession>> SESSIONS = new HashMap<>();

    private static ObjectMapper mapper = new ObjectMapper();

    {
        mapper.setConfig(mapper.getSerializationConfig().with(
                new SimpleDateFormat(DateTimeFormatter.FORMAT)));
    }

    public static void sessionRefreshUser(long userId) {
        synchronized (SESSIONS) {
            SESSIONS.remove(userId);
        }
    }

    public static void sessionRefreshDevice(long deviceId) {
        synchronized (SESSIONS) {
            Iterator<Map.Entry<Long, Map<Session, AsyncSession>>> iterator = SESSIONS.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Long, Map<Session, AsyncSession>> session = iterator.next();
                Iterator<Map.Entry<Session, AsyncSession>> asyncSessions = session.getValue().entrySet().iterator();

                while (asyncSessions.hasNext()) {
                    Map.Entry<Session, AsyncSession> asyncSession = asyncSessions.next();
                    if (asyncSession.getValue().hasDevice(deviceId)) {
                        asyncSessions.remove();
                    }
                }
            }
        }
    }

    public static class AsyncSession {

        private final Set<Long> devices = new HashSet<>();
        private final Set<Device> deviceUpdates = new HashSet<>();
        private final Set<Position> positionUpdates = new HashSet<>();
        private Session session;

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
                    deviceUpdates.add(device);
                    response();
                }
            }

            @Override
            public void onUpdatePosition(Position position) {
                synchronized (AsyncSession.this) {
                    positionUpdates.add(position);
                    response();
                }
            }
        };

        public synchronized void request() {
            if ( ! deviceUpdates.isEmpty() || ! positionUpdates.isEmpty()) {
                response();
            }
        }

        private synchronized void response() {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("devices", deviceUpdates);
                data.put("positions", positionUpdates);

                Message message = new Message("position", data);
                String m = mapper.writeValueAsString(message);

                deviceUpdates.clear();
                positionUpdates.clear();

                session.getAsyncRemote().sendText(m);
                } catch (IOException e) {
                    e.printStackTrace();
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR, "exception"));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        private synchronized void response(Message message) throws IOException {
            try {

                String m = mapper.writeValueAsString(message);
                session.getAsyncRemote().sendText(m);
            } catch (IOException e) {
                e.printStackTrace();
                session.close(new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR, "exception"));
            }
        }

        public synchronized void removeListener(){
            Context.getConnectionManager().removeListener(devices, dataListener);
        }
    }

    @OnOpen
    public void OnOpen(Session session, EndpointConfig endpointConfig) {
        synchronized (SESSIONS) {
            HttpSession httpSession = (HttpSession) endpointConfig.getUserProperties()
                    .get(HttpSession.class.getName());
            Long userId = getUserId(httpSession);

            Collection<Long> devices = Context.getPermissionsManager().allowedDevices(userId);
            if (!SESSIONS.containsKey(userId)) {
                Map<Session, AsyncSession> sessionMap = new HashMap<>();
                sessionMap.put(session, new AsyncSession(devices, session));
                SESSIONS.put(userId, sessionMap);
            } else {
                SESSIONS.get(userId).put(session, new AsyncSession(devices, session));
            }
            Map<Session, AsyncSession> sessionAsyncSessionMap = SESSIONS.get(userId);
            sessionAsyncSessionMap.get(session).request();
        }
    }

    private Long getUserId(HttpSession httpSession) {
        String id = httpSession.getId();
        Long userId = SessionResource.sessions.get(id);
        return userId;
    }

    @OnMessage
    public static  void onMessage(String message, Session session) {
        System.out.println("On Message for Web Socket");
    }

    @OnClose
    public void onClose(Session session) {
        
        /*synchronized (SESSIONS) {
            Long userId = getUserId(session);
            Map<Session, AsyncSession> asyncSession = SESSIONS.get(userId);
            if (asyncSession.containsKey(session)) {
                asyncSession.remove(session).removeListener();
            }
            if (asyncSession.isEmpty()) {
                SESSIONS.remove(userId);
            }
        }*/
    }

    public static void showAlert(Message message){
        Alert body = (Alert)message.getBody();
        Notification notification = (Notification)body.getMessage();
        synchronized (SESSIONS) {
            Iterator<Map.Entry<Long, Map<Session, AsyncSession>>> iterator = SESSIONS.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Long, Map<Session, AsyncSession>> session = iterator.next();
                Iterator<Map.Entry<Session, AsyncSession>> asyncSessions = session.getValue().entrySet().iterator();

                while (asyncSessions.hasNext()) {
                    Map.Entry<Session, AsyncSession> asyncSession = asyncSessions.next();
                    if (asyncSession.getValue().hasDevice(notification.getDeviceId())) {
                        try {
                            asyncSession.getValue().response(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
