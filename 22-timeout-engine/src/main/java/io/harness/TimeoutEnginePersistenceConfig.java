package io.harness;

import io.harness.annotation.HarnessRepo;
import io.harness.springdata.HMongoTemplate;
import io.harness.springdata.SpringPersistenceConfig;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.timeout"},
    includeFilters = @ComponentScan.Filter(HarnessRepo.class), mongoTemplateRef = "timeoutEngineMongoTemplate")
public class TimeoutEnginePersistenceConfig extends SpringPersistenceConfig {
  @Inject
  public TimeoutEnginePersistenceConfig(Injector injector) {
    super(injector, Collections.emptyList());
  }

  @Bean(name = "timeoutEngineMongoTemplate")
  public MongoTemplate mongoTemplate() throws Exception {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(this.mongoDbFactory());
    MongoMappingContext mappingContext = this.mongoMappingContext();
    mappingContext.setAutoIndexCreation(false);
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
    converter.setCustomConversions(collectConverters());
    converter.setCodecRegistryProvider(this.mongoDbFactory());
    converter.afterPropertiesSet();
    return new HMongoTemplate(mongoDbFactory(), mappingMongoConverter());
  }
}
