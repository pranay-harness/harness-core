package io.harness.utils;

import io.harness.functional.AbstractFunctionalTest;
import io.harness.resource.Project;
import lombok.extern.slf4j.Slf4j;
import software.wings.helpers.ext.mail.SmtpConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileUtils {
  public static void modifySMTPInConfigFile(File file, SmtpConfig smtpConfig) throws IOException {
    Path path = Paths.get(file.getParent(), "modified_config.yml");
    File outputFile = new File(path.toString());

    // we need to store all the lines
    List<String> lines = new ArrayList<>();

    // first, read the file and store the changes
    BufferedReader in = new BufferedReader(new FileReader(file));
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
      lines.add(line);
      line = in.readLine();
    }
    in.close();

    // now, write the file again with the changes
    PrintWriter out = new PrintWriter(outputFile);
    for (String l : lines) {
      out.println(l);
    }
    out.close();
  }

  public static void deleteModifiedConfig() {
    final Path config =
        Paths.get(Project.rootDirectory(AbstractFunctionalTest.class), "71-rest", "modified_config.yml");
    File file = new File(config.toString());
    if (file.exists()) {
      logger.info("Deleting the config file as all the tests are completed : " + file.getName());
      file.delete();
    }
  }
}
