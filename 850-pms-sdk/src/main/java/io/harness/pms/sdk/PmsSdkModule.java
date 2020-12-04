package io.harness.pms.sdk;

import static java.time.Duration.ofSeconds;

import io.harness.config.PublisherConfiguration;
import io.harness.data.structure.EmptyPredicate;
import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.queue.QueueFactory;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.plan.InitializeSdkRequest;
import io.harness.pms.plan.PmsServiceGrpc.PmsServiceBlockingStub;
import io.harness.pms.plan.Types;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreationResponseMerger;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.execution.NodeExecutionEventListener;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueController;
import io.harness.queue.QueueListener;
import io.harness.registries.state.StepRegistry;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.PmsSdkModuleRegistrars;
import io.harness.spring.AliasRegistrar;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.*;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.*;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.data.mongodb.core.MongoTemplate;

public class PmsSdkModule {
  private static PmsSdkModule defaultInstance;

  public static PmsSdkModule getDefaultInstance() {
    return defaultInstance;
  }

  public static void initializeDefaultInstance(PmsSdkConfiguration config) {
    if (defaultInstance == null) {
      defaultInstance = new PmsSdkModule(config);
      defaultInstance.initialize();
    }
  }

  private final PmsSdkConfiguration config;

  private PmsSdkModule(PmsSdkConfiguration config) {
    this.config = config;
  }

  private void initialize() {
    PipelineServiceInfoProvider pipelineServiceInfoProvider = config.getPipelineServiceInfoProvider();
    String serviceName = pipelineServiceInfoProvider.getServiceName();

    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      public PmsSdkConfiguration config() {
        return config;
      }
    });
    modules.add(PmsSdkGrpcModule.getInstance());
    modules.add(PmsSdkPersistenceModule.getInstance());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      public QueueConsumer<NodeExecutionEvent> nodeExecutionEventQueueConsumer(
          Injector injector, PublisherConfiguration config) {
        MongoTemplate sdkTemplate =
            injector.getInstance(Key.get(MongoTemplate.class, Names.named("pmsSdkMongoTemplate")));
        return QueueFactory.createNgQueueConsumer(injector, NodeExecutionEvent.class, ofSeconds(5),
            Collections.singletonList(Collections.singletonList(serviceName)), config, sdkTemplate);
      }

      @Provides
      @Singleton
      public Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(PmsSdkModuleRegistrars.kryoRegistrars)
            .build();
      }

      @Provides
      @Singleton
      public Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(PmsSdkModuleRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      public Set<Class<? extends AliasRegistrar>> aliasRegistrars() {
        return ImmutableSet.<Class<? extends AliasRegistrar>>builder()
            .addAll(PmsSdkModuleRegistrars.aliasRegistrars)
            .build();
      }

      @Provides
      @Singleton
      public Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder()
            .addAll(PmsSdkModuleRegistrars.morphiaConverters)
            .build();
      }

      @Provides
      @Singleton
      public PipelineServiceInfoProvider pipelineServiceInfoProvider() {
        return config.getPipelineServiceInfoProvider();
      }

      @Provides
      @Singleton
      public FilterCreationResponseMerger filterCreationResponseMerger() {
        return config.getFilterCreationResponseMerger();
      }

      @Provides
      @Singleton
      public ServiceManager serviceManager(Set<Service> services) {
        return new ServiceManager(services);
      }

      @Provides
      @Singleton
      @Named("pmsSdkMongoConfig")
      public MongoConfig mongoConfig() {
        return config.getMongoConfig();
      }

      @Provides
      @Singleton
      StepRegistry providesStateRegistry() {
        return config.getStepRegistry();
      }
    });
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueController.class).toInstance(new QueueController() {
          @Override
          public boolean isPrimary() {
            return true;
          }

          @Override
          public boolean isNotPrimary() {
            return false;
          }
        });
        bind(new TypeLiteral<QueueListener<NodeExecutionEvent>>() {}).to(NodeExecutionEventListener.class);
      }
    });

    Injector injector = Guice.createInjector(modules);

    ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
    serviceManager.awaitHealthy();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));

    PmsServiceBlockingStub pmsClient = injector.getInstance(PmsServiceBlockingStub.class);
    pmsClient.initializeSdk(InitializeSdkRequest.newBuilder()
                                .setName(serviceName)
                                .putAllSupportedTypes(calculateSupportedTypes(pipelineServiceInfoProvider))
                                .addAllSupportedSteps(pipelineServiceInfoProvider.getStepInfo())
                                .build());
  }

  private Map<String, Types> calculateSupportedTypes(PipelineServiceInfoProvider pipelineServiceInfoProvider) {
    List<PartialPlanCreator<?>> planCreators = pipelineServiceInfoProvider.getPlanCreators();
    if (EmptyPredicate.isEmpty(planCreators)) {
      return Collections.emptyMap();
    }

    Map<String, Set<String>> supportedTypes = new HashMap<>();
    for (PartialPlanCreator<?> planCreator : planCreators) {
      Map<String, Set<String>> currTypes = planCreator.getSupportedTypes();
      if (EmptyPredicate.isEmpty(currTypes)) {
        continue;
      }

      currTypes.forEach((k, v) -> {
        if (EmptyPredicate.isEmpty(v)) {
          return;
        }

        if (supportedTypes.containsKey(k)) {
          supportedTypes.get(k).addAll(v);
        } else {
          supportedTypes.put(k, new HashSet<>(v));
        }
      });
    }

    Map<String, Types> finalMap = new HashMap<>();
    supportedTypes.forEach((k, v) -> finalMap.put(k, Types.newBuilder().addAllTypes(v).build()));
    return finalMap;
  }
}
