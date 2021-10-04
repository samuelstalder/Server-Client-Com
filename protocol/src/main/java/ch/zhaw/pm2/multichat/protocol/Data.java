package ch.zhaw.pm2.multichat.protocol;

import java.io.Serializable;

/**
 * Class carries information about the sent data
 * It's used as communication medium between server and client
 * sender:   certain user
 * receiver: certain user or * (all)
 * type:     NEW, CONFIRM_CONNECT, CONNECTED, CONFIRM_DISCONNECT, DISCONNECTED;
 * payload:  message
 */
public class Data implements Serializable {

    private String sender = null;
    private String receiver = null;
    private String type = null;
    private String payload = null;

    public Data (String sender, String receiver, String type, String payload) {
        this.sender = sender;
        this.receiver = receiver;
        this.type = type;
        this.payload = payload;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }
}
