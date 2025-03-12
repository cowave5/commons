package com.cowave.commons.framework.helper.socketio;

import com.corundumstudio.socketio.SocketIOServer;

/**
 *
 * @author shanhuiming
 *
 */
public interface SocketIoEventRegister {

    void registerEventListener(SocketIOServer socketIoServer);
}
