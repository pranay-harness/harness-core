package io.harness.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Localhost {
  public static String getLocalHostAddress() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      return "127.0.0.1";
    }
  }

  public static String getLocalHostName() {
    try {
      return InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      return "ip-" + getLocalHostAddress().replaceAll("\\.", "-");
    }
  }
}
