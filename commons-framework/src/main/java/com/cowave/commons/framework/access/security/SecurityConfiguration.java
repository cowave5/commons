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

import com.cowave.commons.framework.access.AccessProperties;
import com.cowave.commons.framework.configuration.ApplicationProperties;
import com.cowave.commons.framework.helper.redis.RedisHelper;
import com.cowave.commons.tools.Collections;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author shanhuiming
 */
@ConditionalOnClass(SecurityFilterChain.class)
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final AccessProperties accessProperties;

    private final ApplicationProperties applicationProperties;

    @Nullable
    private final RedisHelper redisHelper;

    @Nullable
    private final BearerTokenService bearerTokenService;

    @ConditionalOnMissingBean(PasswordEncoder.class)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @ConditionalOnMissingBean(UserDetailsService.class)
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return defaultUserDetailsService(passwordEncoder);
    }

    private UserDetailsService defaultUserDetailsService(PasswordEncoder passwordEncoder) {
        List<AccessUser> userList = accessProperties.accessUsers();
        return new AccessUserDetailsServiceImpl(accessProperties.authMode(), passwordEncoder,
                bearerTokenService, applicationProperties, Collections.copyToMap(userList, AccessUser::getUsername));
    }

    @ConditionalOnMissingBean(AuthenticationManager.class)
    @Bean
    public AuthenticationManager authenticationManagerBean(PasswordEncoder passwordEncoder, UserDetailsService userDetailsService) {
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setPasswordEncoder(passwordEncoder);
		authenticationProvider.setUserDetailsService(userDetailsService);
		return new ProviderManager(authenticationProvider);
	}

    @ConditionalOnMissingBean(value = {SecurityFilterChain.class, WebSecurityConfigurerAdapter.class})
    @ConditionalOnProperty(name = "spring.access.auth.mode", havingValue = "basic", matchIfMissing = true)
    @Bean
    public SecurityFilterChain basicSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        // 允许跨域
        httpSecurity.csrf().disable();
        // 无状态会话
        httpSecurity.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        // 取消X-Frame-Options，允许嵌入到<iframe>
        httpSecurity.headers().frameOptions().disable();
        // username:password Base64编码
        httpSecurity.httpBasic();
        if (accessProperties.authEnable()) {
            if (ArrayUtils.isNotEmpty(accessProperties.basicAuthUrls())) {
                httpSecurity.authorizeRequests().antMatchers(accessProperties.basicAuthUrls()).authenticated().anyRequest().permitAll();
            } else if (ArrayUtils.isNotEmpty(accessProperties.basicIgnoreUrls())) {
                httpSecurity.authorizeRequests().antMatchers(accessProperties.basicIgnoreUrls()).permitAll().anyRequest().authenticated();
            } else {
                httpSecurity.authorizeRequests().anyRequest().authenticated();
            }
        } else {
            httpSecurity.authorizeRequests().anyRequest().permitAll();
        }
        return httpSecurity.build();
    }

    @ConditionalOnMissingBean(value = {SecurityFilterChain.class, WebSecurityConfigurerAdapter.class})
    @ConditionalOnProperty(name = "spring.access.auth.mode", havingValue = "accessToken")
    @Bean
    public SecurityFilterChain accessBearerSecurityFilterChain(HttpSecurity httpSecurity,
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) throws Exception {
        return newBearerSecurityFilterChain(httpSecurity, userDetailsService, passwordEncoder, false);
    }

    @ConditionalOnMissingBean(value = {SecurityFilterChain.class, WebSecurityConfigurerAdapter.class})
    @ConditionalOnProperty(name = "spring.access.auth.mode", havingValue = "refreshToken")
    @Bean
    public SecurityFilterChain refreshBearerSecurityFilterChain(HttpSecurity httpSecurity,
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) throws Exception {
        return newBearerSecurityFilterChain(httpSecurity, userDetailsService, passwordEncoder, true);
    }

    private SecurityFilterChain newBearerSecurityFilterChain(HttpSecurity httpSecurity,
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, boolean useRefreshToken) throws Exception {
        // 允许跨域
        httpSecurity.csrf().disable();
        // 无状态会话
        httpSecurity.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        // 取消X-Frame-Options，允许嵌入到<iframe>
        httpSecurity.headers().frameOptions().disable();
        if (accessProperties.authEnable()) {
            String[] basicAuthUrls = accessProperties.basicAuthUrls();
            String[] tokenAuthUrls = accessProperties.tokenAuthUrls();
            String[] tokenIgnoreUrls = accessProperties.tokenIgnoreUrls();
            boolean basicWithConfigUser = accessProperties.basicWithConfigUser();
            String[] ignoreUrls = Stream.of(basicAuthUrls, tokenIgnoreUrls)
                    .filter(Objects::nonNull).flatMap(Arrays::stream).filter(Objects::nonNull).toArray(String[]::new);
            if (ArrayUtils.isNotEmpty(tokenAuthUrls)) {
                httpSecurity.authorizeRequests().antMatchers(tokenAuthUrls).authenticated().anyRequest().permitAll();
            } else if (ArrayUtils.isNotEmpty(ignoreUrls)) {
                httpSecurity.authorizeRequests().antMatchers(ignoreUrls).permitAll().anyRequest().authenticated();
            } else {
                httpSecurity.authorizeRequests().anyRequest().authenticated();
            }

            if (bearerTokenService == null) {
                throw new IllegalStateException("SecurityFilterChain is not initialized，Please ensure the Jwt dependency has imported.");
            }

            if(useRefreshToken && redisHelper == null){
                throw new IllegalStateException("SecurityFilterChain is not initialized，Please ensure the Redis dependency has imported.");
            }

            // Bearer处理
            BearerTokenFilter bearerTokenFilter = new BearerTokenFilter(useRefreshToken, bearerTokenService, tokenAuthUrls, ignoreUrls);
            httpSecurity.addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class);

            // Basic处理
            if (ArrayUtils.isNotEmpty(basicAuthUrls)) {
                UserDetailsService defaultUserDetailsService = defaultUserDetailsService(passwordEncoder);
                BasicAuthFilter basicAuthFilter = new BasicAuthFilter(userDetailsService,
                        defaultUserDetailsService, basicWithConfigUser, passwordEncoder, basicAuthUrls);
                httpSecurity.addFilterBefore(basicAuthFilter, BearerTokenFilter.class);
            }
        } else {
            httpSecurity.authorizeRequests().anyRequest().permitAll();
        }
        return httpSecurity.build();
    }

    @ConditionalOnBean(value = {SecurityFilterChain.class, WebSecurityConfigurerAdapter.class})
    @Bean
    public AccessInfoParser accessUserParser(){
        return new AccessInfoParser();
    }
}
