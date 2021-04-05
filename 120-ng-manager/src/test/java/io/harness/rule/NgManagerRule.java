package io.harness.rule;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.mockito.Mockito.mock;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.gitsync.ConnectorGitSyncHelper;
import io.harness.factory.ClosingFactory;
import io.harness.gitsync.branching.GitBranchingHelper;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.userprofile.commons.SCMType;
import io.harness.ng.userprofile.entities.BitbucketSCM.BitbucketSCMMapper;
import io.harness.ng.userprofile.entities.GithubSCM.GithubSCMMapper;
import io.harness.ng.userprofile.entities.GitlabSCM.GitlabSCMMapper;
import io.harness.ng.userprofile.entities.SourceCodeManager.SourceCodeManagerMapper;
import io.harness.ngpipeline.common.NGPipelineObjectMapperHelper;
import io.harness.persistence.HPersistence;
import io.harness.pms.serializer.jackson.PmsBeansJacksonModule;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
import io.harness.serializer.NextGenRegistrars;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.yaml.YamlSdkModule;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import io.dropwizard.jackson.Jackson;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.converters.TypeConverter;

@OwnedBy(PL)
@Slf4j
public class NgManagerRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  ClosingFactory closingFactory;

  public NgManagerRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());
    List<Module> modules = new ArrayList<>();
    modules.add(mongoTypeModule(annotations));
    modules.add(TestMongoModule.getInstance());
    modules.add(KryoModule.getInstance());
    modules.add(YamlSdkModule.getInstance());
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
        bind(ConnectorGitSyncHelper.class).toInstance(mock(ConnectorGitSyncHelper.class));
        bind(GitBranchingHelper.class).toInstance(mock(GitBranchingHelper.class));
        MapBinder<SCMType, SourceCodeManagerMapper> sourceCodeManagerMapBinder =
            MapBinder.newMapBinder(binder(), SCMType.class, SourceCodeManagerMapper.class);
        sourceCodeManagerMapBinder.addBinding(SCMType.BITBUCKET).to(BitbucketSCMMapper.class);
        sourceCodeManagerMapBinder.addBinding(SCMType.GITLAB).to(GitlabSCMMapper.class);
        sourceCodeManagerMapBinder.addBinding(SCMType.GITHUB).to(GithubSCMMapper.class);
      }
    });
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return NextGenRegistrars.kryoRegistrars;
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return NextGenRegistrars.morphiaRegistrars;
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder()
            .addAll(ManagerRegistrars.morphiaConverters)
            .build();
      }
      @Provides
      @Singleton
      List<YamlSchemaRootClass> yamlSchemaRootClass() {
        return ImmutableList.<YamlSchemaRootClass>builder().addAll(NextGenRegistrars.yamlSchemaRegistrars).build();
      }

      @Provides
      @Named("yaml-schema-mapper")
      @Singleton
      public ObjectMapper getYamlSchemaObjectMapper() {
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        NGPipelineObjectMapperHelper.configureNGObjectMapper(objectMapper);
        objectMapper.registerModule(new PmsBeansJacksonModule());
        return objectMapper;
      }
    });
    return modules;
  }

  @Override
  public void initialize(Injector injector, List<Module> modules) {
    for (Module module : modules) {
      if (module instanceof ServersModule) {
        for (Closeable server : ((ServersModule) module).servers(injector)) {
          closingFactory.addServer(server);
        }
      }
    }
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(log, statement, frameworkMethod, target);
  }
}
