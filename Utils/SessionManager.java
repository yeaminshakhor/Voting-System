package Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.swing.JFrame;

public class SessionManager {
    private static SessionManager instance;
    private Map<String, UserSession> activeSessions;
    
    private SessionManager() {
        activeSessions = new HashMap<>();
    }
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public String createSession(String userId, String userType, JFrame frame) {
        String sessionId = UUID.randomUUID().toString();
        UserSession session = new UserSession(userId, userType, frame, System.currentTimeMillis());
        activeSessions.put(sessionId, session);
        
        // Start session timeout thread
        new Thread(() -> {
            try {
                Thread.sleep(30 * 60 * 1000); // 30 minutes timeout
                if (activeSessions.containsKey(sessionId)) {
                    invalidateSession(sessionId);
                    frame.dispose();
                    // Return to login
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        
        return sessionId;
    }
    
    public boolean isValidSession(String sessionId) {
        UserSession session = activeSessions.get(sessionId);
        if (session == null) return false;
        
        // Check if session expired (30 minutes)
        long currentTime = System.currentTimeMillis();
        return (currentTime - session.getCreationTime()) < (30 * 60 * 1000);
    }
    
    public void invalidateSession(String sessionId) {
        activeSessions.remove(sessionId);
    }
    
    public UserSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    public class UserSession {
        private String userId;
        private String userType;
        private JFrame frame;
        private long creationTime;
        
        public UserSession(String userId, String userType, JFrame frame, long creationTime) {
            this.userId = userId;
            this.userType = userType;
            this.frame = frame;
            this.creationTime = creationTime;
        }
        
        // Getters
        public String getUserId() { return userId; }
        public String getUserType() { return userType; }
        public JFrame getFrame() { return frame; }
        public long getCreationTime() { return creationTime; }
    }
}