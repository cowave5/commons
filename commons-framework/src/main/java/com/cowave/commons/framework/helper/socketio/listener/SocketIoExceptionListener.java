package com.cowave.commons.framework.helper.socketio.listener;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ExceptionListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
public class SocketIoExceptionListener implements ExceptionListener {

    @Override
    public void onEventException(Exception e, List<Object> list, SocketIOClient socketIoClient) {
        log.error("socketIo event error, " + socketIoClient.getHandshakeData().getUrlParams(), e);
    }

    @Override
    public void onDisconnectException(Exception e, SocketIOClient socketIoClient) {
        log.error("socketIo disconnect error, " + socketIoClient.getHandshakeData().getUrlParams(), e);
    }

    @Override
    public void onConnectException(Exception e, SocketIOClient socketIoClient) {
        log.error("socketIo connect error" + socketIoClient.getHandshakeData().getUrlParams(), e);
    }

    @Override
    public void onPingException(Exception e, SocketIOClient socketIoClient) {
        log.error("socketIo ping error" + socketIoClient.getHandshakeData().getUrlParams(), e);
    }

    @Override
    public void onPongException(Exception e, SocketIOClient socketIoClient) {
        log.error("socketIo pong error" + socketIoClient.getHandshakeData().getUrlParams(), e);
    }

    @Override
    public boolean exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) {
        log.error(throwable.getMessage(), throwable);
        return true;
    }
}
