package io.harness.reflection;

import static java.lang.String.format;

import com.google.common.base.Preconditions;

import lombok.experimental.UtilityClass;

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Set;

@UtilityClass
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

  public static void checkHarnessClassBelongToModule(String location, Set<? extends Class> classes) {
    for (Class clazz : classes) {
      if (!CodeUtils.isHarnessClass(clazz)) {
        continue;
      }
      final String clazzLocation = Preconditions.checkNotNull(CodeUtils.location(clazz));
      if (clazzLocation.equals(location)) {
        continue;
      }

      throw new RuntimeException(
          format("%s class should be registered in module %s", clazz.getCanonicalName(), clazzLocation));
    }
  }
}
