package io.harness.ng;

import static com.google.inject.Key.get;
import static com.google.inject.name.Names.named;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;

import com.mongodb.MongoClient;
import io.harness.govern.DependencyModule;
import io.harness.mongo.MongoModule;
import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.AdvancedDatastore;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.guice.annotation.GuiceModule;
import org.springframework.guice.module.BeanFactoryProvider;
import org.springframework.guice.module.SpringModule;

public class PersistenceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(HPersistence.class).to(MongoPersistence.class);
    install(new SpringModule(BeanFactoryProvider.from(SpringMongoConfig.class)));
    installModule(getMongoModule());
  }

  @EnableMongoRepositories(basePackages = "io.harness")
  @EnableMongoAuditing
  @Configuration
  @GuiceModule
  public static class SpringMongoConfig extends AbstractMongoConfiguration {
    private final AdvancedDatastore advancedDatastore;

    @Inject
    public SpringMongoConfig(Injector injector) {
      advancedDatastore = injector.getProvider(get(AdvancedDatastore.class, named("primaryDatastore"))).get();
    }

    @Override
    public MongoClient mongoClient() {
      return advancedDatastore.getMongo();
    }

    @Override
    protected String getDatabaseName() {
      return advancedDatastore.getDB().getName();
    }
  }

  protected Module getMongoModule() {
    return new MongoModule();
  }

  private void installModule(Module module) {
    if (module instanceof DependencyModule) {
      ((DependencyModule) module).cumulativeDependencies().forEach(this ::install);
    } else {
      install(module);
    }
  }
}
