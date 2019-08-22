package io.harness.batch.processing;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, MongoAutoConfiguration.class,
                           MongoDataAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class})
@EnableBatchProcessing
public class BatchProcessingApplication {
  public static void main(String[] args) throws Exception {
    SpringApplication.run(BatchProcessingApplication.class, args);
  }
}
