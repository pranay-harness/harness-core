package io.harness.batch.processing.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.serializer.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Slf4j
@Configuration
public class BatchConfiguration {
  @Bean
  public BatchMainConfig batchMainConfig() throws IOException {
    File configFile = new File("batch-processing-config.yml");
    return new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), BatchMainConfig.class);
  }
}
