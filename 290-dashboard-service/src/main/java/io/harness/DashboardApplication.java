/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.logging.LoggingInitializer.initializeLogging;

import static com.google.common.collect.ImmutableMap.of;

import io.harness.accesscontrol.NGAccessDeniedExceptionMapper;
import io.harness.annotations.dev.OwnedBy;
import io.harness.maintenance.MaintenanceController;
import io.harness.ng.core.exceptionmappers.GenericExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.JerseyViolationExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.WingsExceptionMapperV2;
import io.harness.request.RequestContextFilter;
import io.harness.security.NextGenAuthenticationFilter;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;
import io.harness.token.remote.TokenClient;
import io.harness.utils.NGObjectMapperHelper;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;

@OwnedBy(PL)
@Slf4j
public class DashboardApplication extends Application<DashboardServiceConfig> {
  private final MetricRegistry metricRegistry = new MetricRegistry();

  public static void main(String[] args) throws Exception {
    log.info("Starting Dashboards Application...");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));
    new DashboardApplication().run(args);
  }

  @Override
  public void run(DashboardServiceConfig config, Environment environment) throws Exception {
    log.info("Entering startup maintenance mode");
    MaintenanceController.forceMaintenance(true);

    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(
        1, 10, 500L, TimeUnit.MILLISECONDS, new ThreadFactoryBuilder().setNameFormat("main-app-pool-%d").build()));

    List<Module> modules = new ArrayList<>();
    modules.add(new DashboardServiceModule(config));
    Injector injector = Guice.createInjector(modules);
    registerResources(environment, injector);
    registerOasResource(config, environment, injector);
    registerAuthFilters(config, environment, injector);
    registerCorsFilter(config, environment);
    registerRequestContextFilter(environment);
    registerJerseyProviders(environment, injector);
    // todo @deepak Add the correlation filter
    // todo @deepak Add the register for health check
    MaintenanceController.forceMaintenance(false);
  }

  private void registerRequestContextFilter(Environment environment) {
    environment.jersey().register(new RequestContextFilter());
  }

  @Override
  public void initialize(Bootstrap<DashboardServiceConfig> bootstrap) {
    initializeLogging();
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    configureObjectMapper(bootstrap.getObjectMapper());
    bootstrap.addBundle(new SwaggerBundle<DashboardServiceConfig>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(DashboardServiceConfig appConfig) {
        return appConfig.getSwaggerBundleConfiguration();
      }
    });
    bootstrap.setMetricRegistry(metricRegistry);
  }

  public static void configureObjectMapper(final ObjectMapper mapper) {
    NGObjectMapperHelper.configureNGObjectMapper(mapper);
  }

  private void registerOasResource(DashboardServiceConfig appConfig, Environment environment, Injector injector) {
    OpenApiResource openApiResource = injector.getInstance(OpenApiResource.class);
    openApiResource.setOpenApiConfiguration(getOasConfig(appConfig));
    environment.jersey().register(openApiResource);
  }

  private void registerAuthFilters(DashboardServiceConfig config, Environment environment, Injector injector) {
    Predicate<Pair<ResourceInfo, ContainerRequestContext>> predicate = resourceInfoAndRequest
        -> resourceInfoAndRequest.getKey().getResourceMethod().getAnnotation(NextGenManagerAuth.class) != null
        || resourceInfoAndRequest.getKey().getResourceClass().getAnnotation(NextGenManagerAuth.class) != null;
    Map<String, String> serviceToSecretMapping = new HashMap<>();
    serviceToSecretMapping.put(
        AuthorizationServiceHeader.BEARER.getServiceId(), config.getDashboardSecretsConfig().getJwtAuthSecret());
    serviceToSecretMapping.put(AuthorizationServiceHeader.IDENTITY_SERVICE.getServiceId(),
        config.getDashboardSecretsConfig().getJwtIdentityServiceSecret());
    serviceToSecretMapping.put(AuthorizationServiceHeader.DEFAULT.getServiceId(),
        config.getDashboardSecretsConfig().getNgManagerServiceSecret());
    environment.jersey().register(new NextGenAuthenticationFilter(predicate, null, serviceToSecretMapping,
        injector.getInstance(Key.get(TokenClient.class, Names.named("PRIVILEGED")))));
  }

  private OpenAPIConfiguration getOasConfig(DashboardServiceConfig appConfig) {
    OpenAPI oas = new OpenAPI();
    Info info =
        new Info()
            .title("NextGen Dashboard Aggregation API Reference")
            .description(
                "This is the Open Api Spec 3 for the Dashboard Aggregation Service. This is under active development. Beware of the breaking change with respect to the generated code stub")
            .termsOfService("https://harness.io/terms-of-use/")
            .version("3.0")
            .contact(new Contact().email("contact@harness.io"));
    oas.info(info);
    URL baseurl = null;
    try {
      baseurl = new URL("https", appConfig.getHostname(), appConfig.getBasePathPrefix());
      Server server = new Server();
      server.setUrl(baseurl.toString());
      oas.servers(Collections.singletonList(server));
    } catch (MalformedURLException e) {
      log.error("failed to set baseurl for server, {}/{}", appConfig.hostname, appConfig.getBasePathPrefix());
    }
    Set<String> packages = DashboardServiceConfig.getUniquePackagesContainingResources();
    return new SwaggerConfiguration().openAPI(oas).prettyPrint(true).resourcePackages(packages).scannerClass(
        "io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner");
  }

  private void registerCorsFilter(DashboardServiceConfig appConfig, Environment environment) {
    FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    String allowedOrigins = String.join(",", appConfig.getAllowedOrigins());
    cors.setInitParameters(of("allowedOrigins", allowedOrigins, "allowedHeaders",
        "X-Requested-With,Content-Type,Accept,Origin,Authorization,X-api-key", "allowedMethods",
        "OPTIONS,GET,PUT,POST,DELETE,HEAD", "preflightMaxAge", "86400"));
    cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
  }

  private void registerResources(Environment environment, Injector injector) {
    for (Class<?> resource : DashboardServiceConfig.getResourceClasses()) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }

  private void registerJerseyProviders(Environment environment, Injector injector) {
    environment.jersey().register(JerseyViolationExceptionMapperV2.class);
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(EarlyEofExceptionMapper.class);
    environment.jersey().register(NGAccessDeniedExceptionMapper.class);
    environment.jersey().register(WingsExceptionMapperV2.class);
    environment.jersey().register(GenericExceptionMapperV2.class);
    environment.jersey().register(MultiPartFeature.class);
  }
}
