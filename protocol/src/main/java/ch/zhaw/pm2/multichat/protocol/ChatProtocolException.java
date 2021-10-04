package ch.zhaw.pm2.multichat.protocol;

import java.util.logging.Level;

/**
 * Class provides an exception for all problems related to chat message sending.
 */
public class ChatProtocolException extends Exception {

    private Level level;

    public ChatProtocolException(String message) {
        super(message);
    }
}
