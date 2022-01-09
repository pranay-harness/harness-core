/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.network;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;

@UtilityClass
@Slf4j
public class Localhost {
  private static String ipAddressCache;
  private static String hostnameCache;

  public static String getLocalHostAddress() {
    if (isBlank(ipAddressCache)) {
      ipAddressCache = getIpAddress();
    }
    return ipAddressCache;
  }

  private static String getIpAddress() {
    try {
      String hostIp = InetAddress.getLocalHost().getHostAddress();
      if (isBlank(hostIp)) {
        log.warn("InetAddress host address was empty");
      } else if (hostIp.startsWith("127.")) {
        log.warn("InetAddress host address was in the 127.0.0.0/8 range");
      } else {
        return hostIp;
      }
    } catch (Exception e) {
      log.warn("Exception getting InetAddress host address", e);
    }

    try {
      String address = getAddress();
      if (address == null) {
        log.warn("Didn't find network interface with IPV4 address that is not in 127.0.0.0/8");
      } else {
        return address;
      }
    } catch (Exception e) {
      log.warn("Exception getting IP address from network interfaces", e);
    }

    return "0.0.0.0";
  }

  public static String getLocalHostName() {
    if (isBlank(hostnameCache)) {
      hostnameCache = getHostname();
    }
    return hostnameCache;
  }

  private static String getHostname() {
    try {
      String hostname = executeHostname();
      if (isBlank(hostname)) {
        log.warn("hostname -f command result was empty");
      } else if (hostname.contains(" ") || hostname.equals("localhost")) {
        log.warn("hostname -f command returned: " + hostname);
      } else {
        return hostname;
      }
    } catch (Exception ex) {
      log.warn("hostname -f command threw exception", ex);
    }

    try {
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      if (isBlank(hostname)) {
        log.warn("InetAddress canonical hostname was empty");
      } else if (hostname.equals("localhost")) {
        log.warn("InetAddress canonical hostname was 'localhost'");
      } else {
        return hostname;
      }
    } catch (Exception e) {
      log.warn("InetAddress canonical hostname threw exception", e);
    }

    try {
      String hostname = executeHostnameShort();
      if (isBlank(hostname)) {
        log.warn("hostname -s command result was empty");
      } else if (hostname.contains(" ") || hostname.equals("localhost")) {
        log.warn("hostname -s command returned: " + hostname);
      } else {
        return hostname;
      }
    } catch (Exception ex) {
      log.warn("hostname -s command threw exception", ex);
    }

    try {
      String hostname = InetAddress.getLocalHost().getHostName();
      if (isBlank(hostname)) {
        log.warn("InetAddress short hostname was empty");
      } else if (hostname.equals("localhost")) {
        log.warn("InetAddress short hostname was 'localhost'");
      } else {
        return hostname;
      }
    } catch (Exception e) {
      log.warn("InetAddress short hostname threw exception", e);
    }

    return "ip-" + getLocalHostAddress().replaceAll("\\.", "-") + ".unknown";
  }

  @VisibleForTesting
  static String getAddress() throws SocketException {
    Enumeration<NetworkInterface> nInterfaces = NetworkInterface.getNetworkInterfaces();
    while (nInterfaces.hasMoreElements()) {
      NetworkInterface networkInterface = nInterfaces.nextElement();
      Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
      while (inetAddresses.hasMoreElements()) {
        InetAddress inetAddress = inetAddresses.nextElement();
        String address = inetAddress.getHostAddress();
        if (inetAddress.getAddress().length != 4) {
          log.warn("Enumerated inet address is not length 4: " + address);
        } else if (address.startsWith("127.")) {
          log.warn("Enumerated inet address was in the 127.0.0.0/8 range: " + address);
        } else {
          return address;
        }
      }
    }
    return null;
  }

  @VisibleForTesting
  static String executeHostname() throws IOException, InterruptedException, ExecutionException {
    return new ProcessExecutor()
        .command("hostname", "-f")
        .readOutput(true)
        .start()
        .getFuture()
        .get()
        .getOutput()
        .getLines()
        .get(0);
  }

  @VisibleForTesting
  static String executeHostnameShort() throws IOException, InterruptedException, ExecutionException {
    return new ProcessExecutor()
        .command("hostname", "-s")
        .readOutput(true)
        .start()
        .getFuture()
        .get()
        .getOutput()
        .getLines()
        .get(0);
  }
}
