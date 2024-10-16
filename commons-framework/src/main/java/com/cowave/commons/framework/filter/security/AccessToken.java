/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.filter.security;

import cn.hutool.core.util.IdUtil;
import com.cowave.commons.framework.access.Access;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.feign.codec.ResponseCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 *
 * @author shanhuiming
 *
 */
@Data
public class AccessToken implements UserDetails {

	@Serial
	private static final long serialVersionUID = -3928832861296252415L;

	public static final String KEY = "Token:";

	public static final String TYPE_APP = "app";

	public static final String TYPE_USER = "user";

	public static final String TYPE_LDAP = "ldap";

	public static final String TYPE_OAUTH = "oauth";

	/**
	 * accessToken
	 */
	private String accessToken;

	/**
	 * refreshToken
	 */
	private String refreshToken;

	/**
	 * id 每次Refresh后的id不一样，可以根据id来判断Token是否已被refresh过
	 */
	private String id;

	/**
	 * type
	 */
	private String type;

	/**
	 * 登录iP
	 */
	private String loginIp;

	/**
	 * 登录时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date loginTime;

	/**
	 * 访问ip
	 */
	private String accessIp;

	/**
	 * 访问时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date accessTime;

	/**
	 * 用户id
	 */
	private Long userId;

	/**
	 * 用户code
	 */
	private String userCode;

	/**
	 * 用户账号
	 */
	private String username;

	/**
	 * 用户昵称
	 */
	private String userNick;

	/**
	 * 用户密码
	 */
	@JsonIgnore
	private String userPasswd;

	/**
	 * 用户Roles
	 */
	private List<String> roles;

	/**
	 * 用户Permissions
	 */
	private List<String> permissions;

	/**
	 * 部门id
	 */
	private Long deptId;

	/**
	 * 部门code
	 */
	private String deptCode;

	/**
	 * 部门名称
	 */
	private String deptName;

	/**
	 * 集群id
	 */
	private Integer clusterId;

	/**
	 * 集群level
	 */
	private Integer clusterLevel;

	/**
	 * 集群name
	 */
	private String clusterName;

	/**
	 * 校验码
	 */
	private ResponseCode validCode;

	/**
	 * 校验描述
	 */
	private String validDesc;

	/**
	 * 权限
	 */
	private List<? extends GrantedAuthority> authorities;

	public AccessToken(){

	}

	public AccessToken(ResponseCode validCode, String validDesc) {
		this.validCode = validCode;
		this.validDesc = validDesc;
	}

	@JsonIgnore
	@Override
	public String getPassword() {
		return userPasswd;
	}

	@JsonIgnore
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@JsonIgnore
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@JsonIgnore
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@JsonIgnore
	@Override
	public boolean isEnabled() {
		return true;
	}

	@JsonIgnore
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		if(authorities != null){
			return authorities;
		}
		if(CollectionUtils.isNotEmpty(roles)){
			List<SimpleGrantedAuthority> list = new ArrayList<>(roles.size());
			for(String role : roles){
				list.add(new SimpleGrantedAuthority(role));
			}
			return list;
		}
		return new ArrayList<>();
	}

	public static AccessToken newToken(){
		AccessToken accessToken = new AccessToken();
		accessToken.setLoginIp(Access.accessIp());
		accessToken.setLoginTime(Access.accessTime());
		accessToken.setAccessIp(Access.accessIp());
		accessToken.setAccessTime(Access.accessTime());
		accessToken.setId(IdUtil.fastSimpleUUID());
		return accessToken;
	}
}
