package ch.zhaw.pm2.multichat.protocol;

/**
 * Class provides predefined states to handle connection and communication
 */
public class Config {

    public enum State {
        NEW, CONFIRM_CONNECT, CONNECTED, CONFIRM_DISCONNECT, DISCONNECTED;
    }

    public static final String DATA_TYPE_CONNECT = "CONNECT";
    public static final String DATA_TYPE_CONFIRM = "CONFIRM";
    public static final String DATA_TYPE_DISCONNECT = "DISCONNECT";
    public static final String DATA_TYPE_MESSAGE = "MESSAGE";
    public static final String DATA_TYPE_ERROR = "ERROR";

    public static final String USER_NONE = "";
    public static final String USER_ALL = "*";
}
