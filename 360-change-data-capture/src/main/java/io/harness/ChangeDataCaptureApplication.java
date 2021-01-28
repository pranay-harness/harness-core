package io.harness;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.serializer.YamlUtils;

import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
public class ChangeDataCaptureApplication {
  private final ChangeDataCaptureServiceConfig config;
  private ChangeDataCaptureApplication(ChangeDataCaptureServiceConfig config) {
    this.config = config;
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    log.info("Starting Change Data Capture Application...");

    File configFile = new File(args[0]);
    ChangeDataCaptureServiceConfig config =
        new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), ChangeDataCaptureServiceConfig.class);
    new ChangeDataCaptureApplication(config).run();
  }

  private void run() throws InterruptedException {
    log.info("Starting application using config: {}", config);
  }
}
