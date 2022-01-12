/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.springdata.PersistenceStoreUtils.getMatchingEntities;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.MongoConfig;
import io.harness.persistence.Store;
import io.harness.reflection.HarnessReflections;
import io.harness.springdata.HMongoTemplate;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.guice.annotation.GuiceModule;

@Configuration
@GuiceModule
@EnableMongoRepositories(
    basePackages = {"io.harness.repositories"}, includeFilters = @ComponentScan.Filter(GitSyncableHarnessRepo.class))
@EnableMongoAuditing
@OwnedBy(DX)
public class GitSyncablePersistenceConfig extends AbstractMongoConfiguration {
  private final MongoConfig mongoConfig;
  private final Injector injector;

  public GitSyncablePersistenceConfig(Injector injector) {
    this.injector = injector;
    this.mongoConfig = injector.getInstance(Key.get(MongoConfig.class));
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

  @Bean
  public MongoTemplate mongoTemplate() throws Exception {
    return new HMongoTemplate(mongoDbFactory(), mappingMongoConverter(), mongoConfig.getTraceMode());
  }

  @Bean
  MongoTransactionManager transactionManager(MongoDbFactory dbFactory) {
    return new MongoTransactionManager(dbFactory);
  }

  @Override
  protected Set<Class<?>> getInitialEntitySet() {
    Set<Class<?>> classes = HarnessReflections.get().getTypesAnnotatedWith(TypeAlias.class);
    Store store = null;
    if (Objects.nonNull(mongoConfig.getAliasDBName())) {
      store = Store.builder().name(mongoConfig.getAliasDBName()).build();
    }
    return getMatchingEntities(classes, store);
  }

  @Override
  protected boolean autoIndexCreation() {
    return false;
  }
}
