/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools;

import com.cowave.commons.tools.ssh.Terminal;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;

/**
 * @author jiangbo
 */
public class Hosts {

    /**
     * hostIp
     * @see com.alibaba.nacos.api.utils.NetUtils
     */
    public static String hostIp(){
        InetAddress inetAddress = findFirstNonLoopbackAddress();
        if (inetAddress == null) {
            return "";
        }
        return inetAddress.getHostAddress();
    }

    /**
     * hostName
     * @see com.alibaba.nacos.api.utils.NetUtils
     */
    public static String hostName(){
        InetAddress inetAddress = findFirstNonLoopbackAddress();
        if (inetAddress == null) {
            return "";
        }
        return inetAddress.getHostName();
    }

    /**
     * 本地ip列表
     */
    public static List<String> getLocalIps() throws SocketException {
        List<String> list = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') == -1) {
                    list.add(address.getHostAddress());
                }
            }
        }
        return list;
    }

    /**
     * 本地监听Port列表
     */
    public static List<Integer> getListeningPorts() throws IOException, InterruptedException {
        String os = System.getenv("OS");
        if (StringUtils.isNotBlank(os) && StringUtils.containsIgnoreCase(os, "windows")) {
            return List.of();
        }
        String[] cmd = {"/bin/bash", "-c", "netstat -nltp |awk '{if (NR>2){print $4}}'| awk -F \":\" '{print $NF}' | sort | uniq"};
        Terminal.Result result = Terminal.process(cmd, Map.of());
        if(!result.isSuccess()){
            throw new UnsupportedEncodingException("Hosts.getLocalPorts failed" + result.getError());
        }
        return Arrays.stream(StringUtils.split(result.getOutput(), "\n")).map(Integer::parseInt).toList();
    }

    /**
     * 检测socket连接
     */
    public static boolean validSocket(String ip, Integer port) {
        return validSocket(ip, port, 3 * 1000);
    }

    /**
     * 检测socket连接
     */
    public static boolean validSocket(String ip, Integer port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeout);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static InetAddress findFirstNonLoopbackAddress() {
        InetAddress result = null;

        try {
            int lowest = Integer.MAX_VALUE;
            for (Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
                 nics.hasMoreElements(); ) {
                NetworkInterface ifc = nics.nextElement();
                if (ifc.isUp()) {
                    if (ifc.getIndex() < lowest || result == null) {
                        lowest = ifc.getIndex();
                    } else {
                        continue;
                    }

                    for (Enumeration<InetAddress> addrs = ifc.getInetAddresses(); addrs.hasMoreElements(); ) {
                        InetAddress address = addrs.nextElement();
                        boolean isLegalIpVersion = Boolean.parseBoolean(System.getProperty("java.net.preferIPv6Addresses"))
                                ? address instanceof Inet6Address : address instanceof Inet4Address;
                        if (isLegalIpVersion && !address.isLoopbackAddress()) {
                            result = address;
                        }
                    }

                }
            }
        } catch (Exception ignore) {}
        if (result != null) {
            return result;
        }
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException ignore) {
        }
        return null;
    }
}
