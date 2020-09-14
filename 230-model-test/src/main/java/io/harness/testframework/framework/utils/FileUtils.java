package io.harness.testframework.framework.utils;

import io.harness.resource.Project;
import lombok.extern.slf4j.Slf4j;
import software.wings.cdn.CdnConfig;
import software.wings.helpers.ext.mail.SmtpConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileUtils {
  public static void modifyConfigFile(File file) {
    modifySMTPInConfigFile(file, TestUtils.getDefaultSmtpConfig(), TestUtils.getDefaultCdnConfig());
  }

  public static void modifySMTPInConfigFile(File file, SmtpConfig smtpConfig, CdnConfig config) {
    Path path = Paths.get(file.getParent(), "modified_config.yml");
    File outputFile = new File(path.toString());

    // we need to store all the lines
    List<String> lines = new ArrayList<>();

    try (FileInputStream fis = new FileInputStream(file);
         BufferedReader in = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
      String line = in.readLine();
      while (line != null) {
        if (line.contains("host_placeholder")) {
          line = line.replace("host_placeholder", smtpConfig.getHost());
        }
        if (line.contains("smtp_username_placeholder")) {
          line = line.replace("smtp_username_placeholder", smtpConfig.getUsername());
        }
        if (line.contains("smtp_password_placeholder")) {
          // line = line.replace("password: \"smtp_password_placeholder\"", "encryptedPassword: " +
          // String.valueOf(smtpConfig.getPassword()));
          line = line.replace("\"smtp_password_placeholder\"", String.valueOf(smtpConfig.getPassword()));
        }
        if (line.contains("cdnSignedUrlKey")) {
          line = line.replace("cdnSignedUrlKey", config.getKeySecret());
        }
        lines.add(line);
        line = in.readLine();
      }

      // now, write the file again with the changes
      PrintWriter out = new PrintWriter(outputFile, "UTF-8");
      for (String l : lines) {
        out.println(l);
      }
      out.close();
    } catch (RuntimeException | IOException e) {
      logger.error("Error thrown in framework", e);
    }
  }

  public static void deleteModifiedConfig(Class clazz) {
    final Path config = Paths.get(Project.rootDirectory(clazz), "71-rest", "modified_config.yml");
    File file = new File(config.toString());
    if (file.exists()) {
      logger.info("Deleting the config file as all the tests are completed : " + file.getName());
      if (!file.delete()) {
        logger.error("Configuration files was not deleted");
      }
    }
  }
}
