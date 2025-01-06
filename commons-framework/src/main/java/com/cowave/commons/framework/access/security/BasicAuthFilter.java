/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
public class BasicAuthFilter extends OncePerRequestFilter {
    private final List<AntPathRequestMatcher> authMatchers = new ArrayList<>();
    private final UserDetailsService userDetailsService;
    private final UserDetailsService defaultUserDetailsService;
    private final boolean basicWithConfigUser;
    private final PasswordEncoder passwordEncoder;

    public BasicAuthFilter(UserDetailsService userDetailsService, UserDetailsService defaultUserDetailsService,
                           boolean basicWithConfigUser, PasswordEncoder passwordEncoder, String[] authUrls){
        this.userDetailsService = userDetailsService;
        this.defaultUserDetailsService = defaultUserDetailsService;
        this.basicWithConfigUser = basicWithConfigUser;
        this.passwordEncoder = passwordEncoder;
        if(ArrayUtils.isNotEmpty(authUrls)){
            Arrays.stream(authUrls).map(AntPathRequestMatcher::new).forEach(authMatchers::add);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        for (AntPathRequestMatcher matcher : authMatchers) {
                if (matcher.matches(request)) {
                    basicAuth(request, response, chain);
                    return;
                }
            }
            chain.doFilter(request, response);
    }

    private void basicAuth(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String basicHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (basicHeader == null){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Access to the application\"");
            return;
        }
        if(!basicHeader.startsWith("Basic ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Access to the application\"");
            return;
        }

        basicHeader = new String(Base64.getDecoder().decode(basicHeader.substring(6)), StandardCharsets.UTF_8);
        String[] array = basicHeader.split(":", 2);
        String username = array[0];
        String password = array[1];
        try {
            UserDetails userDetails = null;
            if(basicWithConfigUser){
                userDetails = defaultUserDetailsService.loadUserByUsername(username);
            }else{
                userDetailsService.loadUserByUsername(username);
            }

            if (userDetails == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"User not exist!\"");
                return;
            }
            if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Password not correct!\"");
                return;
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Basic failed!\"");
        }
    }
}
