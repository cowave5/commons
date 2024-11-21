/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access.security;

import com.cowave.commons.framework.access.AccessProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Nullable;
import java.util.List;

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

    @Nullable
    private final BearerTokenService bearerTokenService;

    @ConditionalOnProperty(prefix = "spring.access.token", name = "mode", havingValue = "basic", matchIfMissing = true)
    @ConditionalOnMissingBean(value = {SecurityFilterChain.class, WebSecurityConfigurerAdapter.class})
    @Bean
    public SecurityFilterChain basicSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        // 无状态会话
        httpSecurity.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        // 取消X-Frame-Options，允许嵌入到<iframe>
        httpSecurity.headers().frameOptions().disable();
        // 允许跨域
        httpSecurity.csrf().disable();
        // username:password Base64编码
        httpSecurity.httpBasic();
        // 需要认证的Url
        httpSecurity.authorizeRequests()
                .antMatchers(accessProperties.getSecurityUrls()).authenticated().anyRequest().permitAll();
        return httpSecurity.build();
    }

    @ConditionalOnProperty(prefix = "spring.access.token", name = "mode", havingValue = "basic", matchIfMissing = true)
    @ConditionalOnMissingBean(UserDetailsService.class)
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        List<BasicAuthUser> userList = accessProperties.getSecurityUsers();
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        for (BasicAuthUser user : userList) {
            manager.createUser(User.withUsername(user.getUsername())
                    .password(passwordEncoder.encode(user.getPassword()))
                    .roles(user.getRoles()).build());
        }
        return manager;
    }

    @ConditionalOnProperty(prefix = "spring.access.token", name = "mode", havingValue = "basic", matchIfMissing = true)
    @ConditionalOnMissingBean(PasswordEncoder.class)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @ConditionalOnProperty(prefix = "spring.access.token", name = "mode", havingValue = "access")
    @ConditionalOnMissingBean(value = {SecurityFilterChain.class, WebSecurityConfigurerAdapter.class})
    @Bean
    public SecurityFilterChain bearerSimpleSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return newSecurityFilterChain(httpSecurity, false);
    }

    @ConditionalOnProperty(prefix = "spring.access.token", name = "mode", havingValue = "access-refresh")
    @ConditionalOnMissingBean(value = {SecurityFilterChain.class, WebSecurityConfigurerAdapter.class})
    @Bean
    public SecurityFilterChain bearerDualSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return newSecurityFilterChain(httpSecurity, true);
    }

    private SecurityFilterChain newSecurityFilterChain(HttpSecurity httpSecurity, boolean useRefreshToken) throws Exception {
        httpSecurity.csrf().disable();
        httpSecurity.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        httpSecurity.headers().frameOptions().disable();
        httpSecurity.authorizeRequests().antMatchers(accessProperties.tokenIgnoreUrls()).permitAll().anyRequest().authenticated();
        BearerTokenFilter bearerTokenFilter = new BearerTokenFilter(useRefreshToken, bearerTokenService, accessProperties.tokenIgnoreUrls());
        httpSecurity.addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
}