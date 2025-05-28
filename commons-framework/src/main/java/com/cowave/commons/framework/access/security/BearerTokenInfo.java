package com.cowave.commons.framework.access.security;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 *
 * @author shanhuiming
 *
 */
@NoArgsConstructor
@Data
public class BearerTokenInfo {

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 会话id
     */
    private String accessId;

    /**
     * 会话类型
     */
    private String accessType;

    /**
     * 访问IP
     */
    private String accessIp;

    /**
     * 访问集群
     */
    private String accessCluster;

    /**
     * 访问时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date accessTime;

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * 登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date loginTime;

    public BearerTokenInfo(AccessUserDetails accessUserDetails){
        this.userAccount = accessUserDetails.getUsername();
        this.userName = accessUserDetails.getUserNick();
        this.accessType = accessUserDetails.getType();
        this.accessId = accessUserDetails.getAccessId();
        this.accessIp = accessUserDetails.getAccessIp();
        this.accessTime = accessUserDetails.getAccessTime();
        this.loginIp = accessUserDetails.getLoginIp();
        this.loginTime = accessUserDetails.getLoginTime();
        this.accessCluster = accessUserDetails.getClusterName();
    }
}
