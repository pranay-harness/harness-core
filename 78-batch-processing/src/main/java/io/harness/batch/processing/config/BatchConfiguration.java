package io.harness.batch.processing.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.event.app.EventServiceConfig;
import io.harness.serializer.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@Slf4j
public class BatchConfiguration {
  @Bean
  public EventServiceConfig eventServiceConfig() throws Exception {
    File configFile = new File("batch-processing-config.yml");
    return new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), EventServiceConfig.class);
  }
}
