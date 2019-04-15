package io.harness.resource;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

@Slf4j
public class Project {
  public static String rootDirectory(Class clazz) {
    try {
      File someFile = Paths.get(clazz.getProtectionDomain().getCodeSource().getLocation().toURI())
                          .resolve(Paths.get("someFile"))
                          .toFile();

      while (!someFile.getParentFile().getName().endsWith("classes")
          && !someFile.getParentFile().getParentFile().getName().equals("target")) {
        someFile = someFile.getParentFile();
      }

      return someFile.getParentFile().getParentFile().getParentFile().getParentFile().getAbsolutePath();
    } catch (URISyntaxException e) {
      logger.error("This should never happen", e);
    }

    return null;
  }
}
