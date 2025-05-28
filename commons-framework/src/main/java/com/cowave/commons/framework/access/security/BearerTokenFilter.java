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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 *
 * @author shanhuiming
 *
 */
public class BearerTokenFilter extends OncePerRequestFilter {

    private final List<AntPathRequestMatcher> permitAllMatchers = new ArrayList<>();

    private final BearerTokenService bearerTokenService;

    private final boolean useRefreshToken;

    public BearerTokenFilter(boolean useRefreshToken, BearerTokenService bearerTokenService, String[] ignoreUrls) {
        this.useRefreshToken = useRefreshToken;
        this.bearerTokenService = bearerTokenService;
        if(ArrayUtils.isNotEmpty(ignoreUrls)){
            Arrays.stream(ignoreUrls).map(AntPathRequestMatcher::new).forEach(permitAllMatchers::add);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        for (AntPathRequestMatcher matcher : permitAllMatchers) {
            if (matcher.matches(request)) {
                chain.doFilter(request, response);
                return;
            }
        }
        bearerAuth(request, response, chain);
    }

    private void bearerAuth(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        AccessUserDetails accessUserDetails;
        if (useRefreshToken) {
            accessUserDetails = bearerTokenService.parseAccessRefreshToken(response);
        } else {
            accessUserDetails = bearerTokenService.parseAccessToken(response);
        }
        if (accessUserDetails == null) {
            return;
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(accessUserDetails, null, accessUserDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }
}
