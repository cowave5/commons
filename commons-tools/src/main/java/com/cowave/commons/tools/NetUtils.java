package com.cowave.commons.tools;

import lombok.Getter;

import java.net.*;
import java.util.*;

/**
 * @author shanhuiming
 */
public class NetUtils {

    /**
     * ip转int
     */
    public static int ipv4ToInt(String ip) {
        String[] parts = ip.trim().split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid ip format: " + ip);
        }
        int result = 0;
        for (String part : parts) {
            result = (result << 8) | Integer.parseInt(part);
        }
        return result;
    }

    /**
     * int转ip
     */
    public static String intToIpv4(int ipInt) {
        return String.format("%d.%d.%d.%d",
                (ipInt >>> 24) & 0xFF,
                (ipInt >>> 16) & 0xFF,
                (ipInt >>> 8) & 0xFF,
                ipInt & 0xFF);
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

    @Getter
    public static class IpMask {
        private final int network;
        private final int mask;
        public IpMask(String ipMask) {
            String[] parts = ipMask.split("/");
            String ip = parts[0];
            int prefix = parts.length > 1 ? Integer.parseInt(parts[1]) : 32;
            this.mask = prefix == 0 ? 0 : -(1 << (32 - prefix));
            this.network = ipv4ToInt(ip) & mask;
        }

        public boolean contains(String ip){
            return contains(ipv4ToInt(ip));
        }

        public boolean contains(int ipInt) {
            return (ipInt & mask) == network;
        }
    }

    /**
     * hostIp
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
     */
    public static String hostName(){
        InetAddress inetAddress = findFirstNonLoopbackAddress();
        if (inetAddress == null) {
            return "";
        }
        return inetAddress.getHostName();
    }

    private static InetAddress findFirstNonLoopbackAddress() {
        InetAddress result = null;

        try {
            int lowest = Integer.MAX_VALUE;
            for (Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces(); nics.hasMoreElements();) {
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
