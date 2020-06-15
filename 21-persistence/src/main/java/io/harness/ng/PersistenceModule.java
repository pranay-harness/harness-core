package io.harness.ng;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import io.harness.govern.DependencyModule;
import io.harness.mongo.MongoModule;
import org.springframework.guice.module.BeanFactoryProvider;
import org.springframework.guice.module.SpringModule;

public class PersistenceModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new SpringModule(BeanFactoryProvider.from(SpringMongoConfig.class)));
    installModule(getMongoModule());
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