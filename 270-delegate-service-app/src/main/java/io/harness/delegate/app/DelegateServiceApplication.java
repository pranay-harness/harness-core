package io.harness.delegate.app;

import static io.harness.annotations.dev.HarnessModule._420_DELEGATE_SERVICE;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.delegate.beans.TaskGroup.GCB;
import static io.harness.logging.LoggingInitializer.initializeLogging;

import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.serializer.AnnotationAwareJsonSubtypeResolver;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;

import software.wings.app.InspectCommand;
import software.wings.app.MainConfiguration;
import software.wings.app.MainConfiguration.AssetsConfigurationMixin;
import software.wings.app.WingsApplication;
import software.wings.beans.GcpConfig;
import software.wings.beans.command.GcbTaskParams;
import software.wings.beans.command.GcbTaskParams.GcbTaskType;
import software.wings.jersey.JsonViews;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.dirkraft.dropwizard.fileassets.FileAssetsBundle;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.dropwizard.Application;
import io.dropwizard.bundles.assets.AssetsConfiguration;
import io.dropwizard.bundles.assets.ConfiguredAssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(_420_DELEGATE_SERVICE)
public class DelegateServiceApplication extends Application<DelegateServiceConfig> {
  public static void main(String... args) throws Exception {
    new DelegateServiceApplication().run(args);
  }

  @Override
  public void run(DelegateServiceConfig delegateServiceConfig, Environment environment) throws Exception {
    log.info("Starting Delegate Service App");
    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(
        20, 1000, 500L, TimeUnit.MILLISECONDS, new ThreadFactoryBuilder().setNameFormat("main-app-pool-%d").build()));

    ObjectMapper mapper = Jackson.newObjectMapper(new YAMLFactory());
    configureObjectMapper(mapper);

    MainConfiguration configuration =
        mapper.readValue(new File("/Users/jennyjames/dev3/portal/360-cg-manager/config_delegate_svc.yml"),
            MainConfiguration.class);
    List<Module> modules = new ArrayList<>();
    //modules.add(new DelegateServiceModule(delegateServiceConfig));
    WingsApplication wingsApplication = new WingsApplication();
    wingsApplication.addModules(configuration, environment, modules);
    Injector injector = Guice.createInjector(modules);
    wingsApplication.initializeManagerSvc(injector, environment, configuration);
    //    registerAtmosphereStreams(environment, injector);
    //    initializegRPCServer(injector);

    Thread thread = new Thread(() -> {
      try {
        Thread.sleep(120000L);
        DelegateTaskServiceClassic delegateTaskServiceClassic = injector.getInstance(DelegateTaskServiceClassic.class);
        delegateTaskServiceClassic.executeTask(
            DelegateTask.builder()
                .accountId("kmpySmUISimoRrJL6NL73w")
                .data(TaskData.builder()
                          .async(true)
                          .taskType(GCB.name())
                          .parameters(new Object[] {GcbTaskParams.builder()
                                                        .gcpConfig(new GcpConfig())
                                                        .encryptedDataDetails(null)
                                                        .type(GcbTaskType.FETCH_TRIGGERS)
                                                        .build()})
                          .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                          .build())
                .build());
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    thread.start();
    log.info("Starting Delegate Service App done");
  }

  @Override
  public String getName() {
    return "Delegate Service Application";
  }

  @Override
  public void initialize(Bootstrap<DelegateServiceConfig> bootstrap) {
    initializeLogging();
    log.info("bootstrapping ...");
    bootstrap.addCommand(new InspectCommand<>(this));

    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.addBundle(new ConfiguredAssetsBundle("/static", "/", "index.html"));
    bootstrap.addBundle(new SwaggerBundle<DelegateServiceConfig>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(DelegateServiceConfig delegateServiceConfig) {
        return delegateServiceConfig.getSwaggerBundleConfiguration();
      }
    });
    bootstrap.addBundle(new FileAssetsBundle("/.well-known"));
    //    ObjectMapper mapper = Jackson.newObjectMapper(new YAMLFactory());
    //    bootstrap.setObjectMapper(mapper);
    configureObjectMapper(bootstrap.getObjectMapper());
    //    this.bootstrap = bootstrap;
    //    bootstrap.setMetricRegistry(metricRegistry);

    log.info("bootstrapping done.");
  }

  public static void configureObjectMapper(final ObjectMapper mapper) {
    mapper.addMixIn(AssetsConfiguration.class, AssetsConfigurationMixin.class);
    final AnnotationAwareJsonSubtypeResolver subtypeResolver =
        AnnotationAwareJsonSubtypeResolver.newInstance(mapper.getSubtypeResolver());
    mapper.setSubtypeResolver(subtypeResolver);
    mapper.setConfig(mapper.getSerializationConfig().withView(JsonViews.Public.class));
    mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
      // defining a different serialVersionUID then base. We don't care about serializing it.
      private static final long serialVersionUID = 7777451630128399020L;
      @Override
      public List<NamedType> findSubtypes(Annotated a) {
        final List<NamedType> subtypesFromSuper = super.findSubtypes(a);
        if (isNotEmpty(subtypesFromSuper)) {
          return subtypesFromSuper;
        }
        return emptyIfNull(subtypeResolver.findSubtypes(a));
      }
    });
  }

  private void initializegRPCServer(Injector injector) {
    log.info("Initializing gRPC Server on Delegate Service application");
    injector.getInstance(ServiceManager.class);
    ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
    serviceManager.awaitHealthy();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));
  }
}
