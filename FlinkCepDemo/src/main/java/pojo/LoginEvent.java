package pojo;

/**
 * @create 2023/9/18
 * @create 16:41
 */
public class LoginEvent {
    public String userId;
    public String ipAddress;
    public String eventTypes;
    public Long timestamp;

    public LoginEvent(String userId, String ipAddress, String eventTypes, Long timestamp) {
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.eventTypes = eventTypes;
        this.timestamp = timestamp;
    }

    public LoginEvent() {
    }

    @Override
    public String toString() {
        return "LoginEvent{" +
                "userId='" + userId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", eventTypes='" + eventTypes + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
