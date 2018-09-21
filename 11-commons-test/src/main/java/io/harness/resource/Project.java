package io.harness.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.nio.file.Paths;

public class Project {
  private static final Logger logger = LoggerFactory.getLogger(Project.class);

  public static String rootDirectory() {
    try {
      return Paths.get(Project.class.getProtectionDomain().getCodeSource().getLocation().toURI())
          .resolve(Paths.get("somefile"))
          .toFile()
          .getParentFile()
          .getParentFile()
          .getParentFile()
          .getParentFile()
          .getAbsolutePath();
    } catch (URISyntaxException e) {
      logger.error("This should never happen", e);
    }

    return null;
  }
}
