package com.cowave.commons.framework.helper.socketio.listener;

import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.HandshakeData;
import com.cowave.commons.framework.access.AccessProperties;
import com.cowave.commons.framework.access.security.BearerTokenService;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
public class SocketIoAuthorizationListener implements AuthorizationListener {

    private final AccessProperties accessProperties;

    @Nullable
    private final BearerTokenService bearerTokenService;

    @Override
    public boolean isAuthorized(HandshakeData handshakeData) {
        if(bearerTokenService == null){
            return true;
        }
        String authorization = handshakeData.getSingleUrlParam(accessProperties.tokenKey());
        return bearerTokenService.validAccessJwt(authorization);
    }
}
