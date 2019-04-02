package io.harness.reflection;

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class CodeUtils {
  public static String location(Class clazz) {
    final ProtectionDomain protectionDomain = clazz.getProtectionDomain();
    if (protectionDomain == null) {
      return null;
    }

    final CodeSource codeSource = protectionDomain.getCodeSource();
    if (codeSource == null) {
      return null;
    }

    final URL location = codeSource.getLocation();
    if (location == null) {
      return null;
    }
    return location.toString();
  }

  public static boolean isHarnessClass(Class clazz) {
    final String pkg = clazz.getCanonicalName();
    if (pkg == null) {
      return false;
    }
    if (pkg.startsWith("io.harness.")) {
      return true;
    }
    if (pkg.startsWith("software.wings.")) {
      return true;
    }
    return false;
  }
}
