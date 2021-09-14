/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resource;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class Project {
  public static String moduleDirectory(Class clazz) {
    try {
      File someFile = Paths.get(clazz.getProtectionDomain().getCodeSource().getLocation().toURI())
                          .resolve(Paths.get("someFile"))
                          .toFile();

      while (!someFile.getParentFile().getName().endsWith("classes")
          && !someFile.getParentFile().getParentFile().getName().equals("target")
          && !someFile.getParentFile().getParentFile().getParentFile().getName().equals("bin")) {
        someFile = someFile.getParentFile();
      }
      // condition is applied so that ScmSecretTest works in both maven and bazel environment
      if (someFile.getParentFile().getParentFile().getParentFile().getName().equals("bin")) {
        File modulePath = someFile.getParentFile().getParentFile();
        return modulePath.getParentFile().getParentFile().getParentFile().getParentFile().getAbsolutePath() + "/"
            + modulePath.getName();
      } else {
        return someFile.getParentFile().getParentFile().getParentFile().getAbsolutePath();
      }
    } catch (URISyntaxException e) {
      log.error("This should never happen", e);
    }

    return null;
  }

  public static String rootDirectory(Class clazz) {
    return new File(moduleDirectory(clazz)).getParentFile().toString();
  }
}
