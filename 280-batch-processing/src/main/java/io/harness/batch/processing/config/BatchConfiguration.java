package io.harness.batch.processing.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.serializer.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.io.File;
import java.io.IOException;

@Slf4j
@Configuration
public class BatchConfiguration {
  @Bean
  public BatchMainConfig batchMainConfig(Environment environment) throws IOException {
    String configFilePath = environment.getProperty("config-file", "batch-processing-config.yml");
    File configFile = new File(configFilePath);
    return new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), BatchMainConfig.class);
  }

  @Bean
  public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.setPoolSize(5);
    return threadPoolTaskScheduler;
  }
}
