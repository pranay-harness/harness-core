package io.harness.network;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.annotations.VisibleForTesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Localhost {
  private static final Logger logger = LoggerFactory.getLogger(Localhost.class);

  public static String getLocalHostAddress() {
    try {
      String address = getAddress();
      if (address != null) {
        return address;
      } else {
        logger.warn("Didn't find network interface with IPV4 address that is not in 127.0.0.0/8");
      }
    } catch (Exception e) {
      logger.warn("Exception getting IP address from network interfaces", e);
    }

    try {
      String hostIp = InetAddress.getLocalHost().getHostAddress();
      if (isNotBlank(hostIp) && !hostIp.startsWith("127.")) {
        return hostIp;
      } else {
        logger.warn("InetAddress host address was blank or was in the 127.0.0.0/8 range");
      }
    } catch (Exception e) {
      logger.error("Exception getting InetAddress host address", e);
    }

    return "0.0.0.0";
  }

  public static String getLocalHostName() {
    try {
      String hostname = executeHostname();
      if (isNotBlank(hostname)) {
        return hostname;
      } else {
        logger.warn("hostname command returned empty");
      }
    } catch (Exception ex) {
      logger.warn("hostname command threw exception", ex);
    }

    try {
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      if (isNotBlank(hostname) && !"localhost".equals(hostname)) {
        return hostname;
      } else {
        logger.warn("InetAddress hostname was blank or 'localhost'");
      }
    } catch (Exception e) {
      logger.warn("InetAddress hostname threw exception", e);
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
        if (inetAddress.getAddress().length == 4 && !address.startsWith("127.")) {
          return address;
        }
      }
    }
    return null;
  }

  @VisibleForTesting
  static String executeHostname() throws IOException, InterruptedException, ExecutionException {
    return new ProcessExecutor()
        .timeout(2, TimeUnit.SECONDS)
        .command("hostname", "-f")
        .readOutput(true)
        .start()
        .getFuture()
        .get()
        .getOutput()
        .getLines()
        .get(0);
  }
}
