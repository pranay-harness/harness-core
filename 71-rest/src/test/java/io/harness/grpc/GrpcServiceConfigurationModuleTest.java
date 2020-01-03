package io.harness.grpc;

import static io.harness.rule.OwnerRule.HANTANG;

import com.google.common.io.Resources;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.harness.category.element.E2ETests;
import io.harness.govern.ProviderModule;
import io.harness.perpetualtask.PerpetualTaskServiceModule;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.wings.app.MainConfiguration;
import software.wings.integration.BaseIntegrationTest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Validator;

@Slf4j
@RunWith(JUnit4.class)
public class GrpcServiceConfigurationModuleTest extends BaseIntegrationTest {
  final ObjectMapper objectMapper = Jackson.newObjectMapper();
  final Validator validator = Validators.newValidator();
  final YamlConfigurationFactory<MainConfiguration> factory =
      new YamlConfigurationFactory<>(MainConfiguration.class, validator, objectMapper, "dw");

  @Test
  @Owner(developers = HANTANG)
  @Category(E2ETests.class)
  @Ignore("This test should be enabled after the gRPC module removes dependency on PerpetualTaskService.")
  public void test() throws Exception {
    final File yaml = new File(Resources.getResource("config.yml").toURI());
    final MainConfiguration configuration = factory.build(yaml);

    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      public GrpcServerConfig getGrpcServerConfig() {
        return configuration.getGrpcServerConfig();
      }
    });
    modules.add(new PerpetualTaskServiceModule());
    modules.add(new GrpcServiceConfigurationModule(configuration.getGrpcServerConfig()));
    Injector injector = Guice.createInjector(modules);
    injector.getInstance(ServiceManager.class).startAsync().awaitHealthy();
  }
}
