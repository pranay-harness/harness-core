package io.harness.batch.processing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(excludeFilters =
        {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = {DataSourceAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
                                EmbeddedMongoAutoConfiguration.class, HazelcastAutoConfiguration.class})
        })
@EnableBatchProcessing(modular = true)
@Slf4j
public class BatchProcessingApplication {
  public static void main(String[] args) {
    new SpringApplicationBuilder(BatchProcessingApplication.class).web(WebApplicationType.NONE).run(args);
  }
}