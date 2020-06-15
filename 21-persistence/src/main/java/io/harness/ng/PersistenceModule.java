package io.harness.ng;

import static com.google.inject.Key.get;
import static com.google.inject.name.Names.named;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;

import com.mongodb.MongoClient;
import io.harness.annotation.HarnessRepo;
import io.harness.govern.DependencyModule;
import io.harness.mongo.MongoModule;
import org.mongodb.morphia.AdvancedDatastore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.guice.annotation.GuiceModule;
import org.springframework.guice.module.BeanFactoryProvider;
import org.springframework.guice.module.SpringModule;

import java.util.Collection;

public class PersistenceModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new SpringModule(BeanFactoryProvider.from(SpringMongoConfig.class)));
    installModule(getMongoModule());
  }

  @EnableMongoRepositories(
      basePackages = {"io.harness"}, mongoTemplateRef = "primary", includeFilters = @Filter(HarnessRepo.class))
  @EnableMongoAuditing
  @Configuration
  @GuiceModule
  public static class SpringMongoConfig extends AbstractMongoConfiguration {
    private final AdvancedDatastore advancedDatastore;
    private static final Collection<String> BASE_PACKAGES = ImmutableList.of("io.harness");

    @Inject
    public SpringMongoConfig(Injector injector) {
      advancedDatastore = injector.getProvider(get(AdvancedDatastore.class, named("primaryDatastore"))).get();
    }

    @Bean(name = "primary")
    @Primary
    public MongoTemplate primaryMongoTemplate() {
      return new MongoTemplate(mongoClient(), getDatabaseName());
    }

    @Override
    public MongoClient mongoClient() {
      return advancedDatastore.getMongo();
    }

    @Override
    protected String getDatabaseName() {
      return advancedDatastore.getDB().getName();
    }

    @Override
    protected Collection<String> getMappingBasePackages() {
      return BASE_PACKAGES;
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

  /* [secondary-db]:
  Uncomment the below section of code if you want to use another DB
  what it essentially does is that it creates a new mongo template and advises spring to use this template for
  repositories under io.harness.ng.core.dao.api.secondary package
  */

  /*
  @EnableMongoRepositories(basePackages = "io.harness.ng.core.dao.api.secondary", mongoTemplateRef = "secondary")
  @Configuration
  @GuiceModule
  public static class SpringMongoConfig1 {
    private AdvancedDatastore advancedDatastore;

    @Inject
    public SpringMongoConfig1(Injector injector) {
      advancedDatastore = injector.getProvider(get(AdvancedDatastore.class, named("secondaryDatastore"))).get();
    }

    @Bean(name = "secondary")
    public MongoTemplate secondaryMongoTemplate() {
      return new MongoTemplate(advancedDatastore.getMongo(), advancedDatastore.getDB().getName());
    }
  }
  */
}