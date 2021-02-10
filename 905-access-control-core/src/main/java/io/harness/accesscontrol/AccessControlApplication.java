package io.harness.accesscontrol;

import static io.harness.accesscontrol.AccessControlConfiguration.getResourceClasses;
import static io.harness.logging.LoggingInitializer.initializeLogging;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.stream.Collectors.toSet;

import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.permissions.PermissionService;
import io.harness.accesscontrol.permissions.PermissionStatus;
import io.harness.accesscontrol.scopes.HarnessScope;
import io.harness.maintenance.MaintenanceController;
import io.harness.metrics.MetricRegistryModule;
import io.harness.ng.core.CorrelationFilter;
import io.harness.ng.core.exceptionmappers.GenericExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.JerseyViolationExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.WingsExceptionMapperV2;
import io.harness.persistence.HPersistence;
import io.harness.remote.CharsetResponseFilter;
import io.harness.remote.NGObjectMapperHelper;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;

@Slf4j
public class AccessControlApplication extends Application<AccessControlConfiguration> {
  private static final String APPLICATION_NAME = "Access Control Service";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));
    new AccessControlApplication().run(args);
  }

  private final MetricRegistry metricRegistry = new MetricRegistry();

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<AccessControlConfiguration> bootstrap) {
    initializeLogging();
    bootstrap.addBundle(new SwaggerBundle<AccessControlConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(AccessControlConfiguration appConfig) {
        return getSwaggerConfiguration();
      }
    });
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    configureObjectMapper(bootstrap.getObjectMapper());
  }

  public static void configureObjectMapper(final ObjectMapper mapper) {
    NGObjectMapperHelper.configureNGObjectMapper(mapper);
  }

  @Override
  public void run(AccessControlConfiguration appConfig, Environment environment) {
    log.info("Starting Access Control Application ...");
    MaintenanceController.forceMaintenance(true);
    Injector injector =
        Guice.createInjector(AccessControlModule.getInstance(appConfig), new MetricRegistryModule(metricRegistry));
    injector.getInstance(HPersistence.class);
    registerCorsFilter(appConfig, environment);
    registerResources(environment, injector);
    registerJerseyProviders(environment);
    registerJerseyFeatures(environment);
    registerCharsetResponseFilter(environment, injector);
    registerCorrelationFilter(environment, injector);

    // TODO: Remove once permission management through yaml is present.
    PermissionService permissionService = injector.getInstance(PermissionService.class);
    Set<String> scopes = new HashSet<>();
    scopes.add(HarnessScope.ACCOUNT.getPathKey());
    scopes.add(HarnessScope.ORGANIZATION.getPathKey());
    scopes.add(HarnessScope.PROJECT.getPathKey());
    Permission permission = Permission.builder()
                                .identifier("core.project.view")
                                .name("View Project")
                                .status(PermissionStatus.ACTIVE)
                                .scopes(scopes)
                                .build();
    permissionService.get(permission.getIdentifier()).orElseGet(() -> permissionService.create(permission));

    MaintenanceController.forceMaintenance(false);
  }

  private void registerJerseyFeatures(Environment environment) {
    environment.jersey().register(MultiPartFeature.class);
  }

  private void registerCorsFilter(AccessControlConfiguration appConfig, Environment environment) {
    Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    String allowedOrigins = String.join(",", appConfig.getAllowedOrigins());
    cors.setInitParameters(of("allowedOrigins", allowedOrigins, "allowedHeaders",
        "X-Requested-With,Content-Type,Accept,Origin,Authorization,X-api-key", "allowedMethods",
        "OPTIONS,GET,PUT,POST,DELETE,HEAD", "preflightMaxAge", "86400"));
    cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
  }

  private void registerResources(Environment environment, Injector injector) {
    for (Class<?> resource : getResourceClasses()) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }

  private void registerJerseyProviders(Environment environment) {
    environment.jersey().register(JerseyViolationExceptionMapperV2.class);
    environment.jersey().register(WingsExceptionMapperV2.class);
    environment.jersey().register(GenericExceptionMapperV2.class);
  }

  private void registerCharsetResponseFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CharsetResponseFilter.class));
  }

  private void registerCorrelationFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CorrelationFilter.class));
  }

  public SwaggerBundleConfiguration getSwaggerConfiguration() {
    SwaggerBundleConfiguration defaultSwaggerBundleConfiguration = new SwaggerBundleConfiguration();
    String resourcePackage = String.join(",", getUniquePackages(getResourceClasses()));
    defaultSwaggerBundleConfiguration.setResourcePackage(resourcePackage);
    defaultSwaggerBundleConfiguration.setSchemes(new String[] {"https", "http"});
    defaultSwaggerBundleConfiguration.setVersion("1.0");
    defaultSwaggerBundleConfiguration.setSchemes(new String[] {"https", "http"});
    defaultSwaggerBundleConfiguration.setHost("{{host}}");
    defaultSwaggerBundleConfiguration.setTitle("Access Control Service API Reference");
    return defaultSwaggerBundleConfiguration;
  }

  private static Set<String> getUniquePackages(Collection<Class<?>> classes) {
    return classes.stream().map(aClass -> aClass.getPackage().getName()).collect(toSet());
  }
}
