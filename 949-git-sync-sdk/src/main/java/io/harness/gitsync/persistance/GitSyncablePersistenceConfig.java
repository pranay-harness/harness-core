package io.harness.gitsync.persistance;

import io.harness.mongo.MongoConfig;
import io.harness.springdata.HMongoTemplate;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import java.util.Collection;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.guice.annotation.GuiceModule;

@Configuration
@GuiceModule
@EnableMongoRepositories(basePackages = {"io.harness.repositories"},
    includeFilters = @ComponentScan.Filter(GitSyncableHarnessRepo.class), mongoTemplateRef = "primary")
@EnableMongoAuditing
public class GitSyncablePersistenceConfig extends AbstractMongoConfiguration {
  private final MongoConfig mongoConfig;
  private final Injector injector;
  //    private final GitAwarePersistence gitAwarePersistence;

  public GitSyncablePersistenceConfig(Injector injector) {
    this.injector = injector;
    this.mongoConfig = injector.getInstance(Key.get(MongoConfig.class));
    // this.gitAwarePersistence = injector.getInstance(Key.get(GitAwarePersistence.class));
  }

  @Override
  public MongoClient mongoClient() {
    MongoClientOptions primaryMongoClientOptions = MongoClientOptions.builder()
                                                       .retryWrites(true)
                                                       .connectTimeout(mongoConfig.getConnectTimeout())
                                                       .serverSelectionTimeout(mongoConfig.getServerSelectionTimeout())
                                                       .maxConnectionIdleTime(mongoConfig.getMaxConnectionIdleTime())
                                                       .connectionsPerHost(mongoConfig.getConnectionsPerHost())
                                                       .readPreference(ReadPreference.primary())
                                                       .build();
    MongoClientURI uri =
        new MongoClientURI(mongoConfig.getUri(), MongoClientOptions.builder(primaryMongoClientOptions));
    return new MongoClient(uri);
  }

  @Override
  protected String getDatabaseName() {
    return new MongoClientURI(mongoConfig.getUri()).getDatabase();
  }

  @Bean(name = "primary")
  @Primary
  public MongoTemplate mongoTemplate() throws Exception {
    return new HMongoTemplate(mongoDbFactory(), mappingMongoConverter());
  }
  ////
  //    @Bean
  //    public GitAwarePersistence gitAwarePersistence() {
  //        return new GitAwarePersistenceImpl();
  //    }

  @Bean
  MongoTransactionManager transactionManager(MongoDbFactory dbFactory) {
    return new MongoTransactionManager(dbFactory);
  }

  @Override
  protected Collection<String> getMappingBasePackages() {
    return Collections.singleton("io.harness");
  }

  @Override
  protected boolean autoIndexCreation() {
    return false;
  }
}