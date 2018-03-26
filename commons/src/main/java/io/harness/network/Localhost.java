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
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Localhost {
  private static final Logger logger = LoggerFactory.getLogger(Localhost.class);

  public static String getLocalHostAddress() {
    try {
      String hostIP = InetAddress.getLocalHost().getHostAddress();
      if (!hostIP.startsWith("127.")) {
        return hostIP;
      }

      String address = getAddress();
      if (address != null) {
        return address;
      }
    } catch (Exception e) {
      logger.error("Couldn't get host address", e);
    }
    return "0.0.0.0";
  }

  public static String getLocalHostName() {
    try {
      String hostname = executeHostname();
      if (isNotBlank(hostname)) {
        return hostname;
      }
    } catch (Exception ex) {
      logger.error("Couldn't get hostname", ex);
    }
    try {
      return InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      return "ip-" + getLocalHostAddress().replaceAll("\\.", "-");
    }
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
