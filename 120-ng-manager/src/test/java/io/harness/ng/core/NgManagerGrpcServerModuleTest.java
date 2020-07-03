package io.harness.ng.core;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;

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
import io.harness.category.element.UnitTests;
import io.harness.govern.ProviderModule;
import io.harness.grpc.GrpcServerConfig;
import io.harness.ng.NextGenConfiguration;
import io.harness.ng.core.remote.server.grpc.NgDelegateTaskResponseGrpcServer;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Validator;

@Slf4j
@RunWith(JUnit4.class)
public class NgManagerGrpcServerModuleTest extends BaseTest {
  final ObjectMapper objectMapper = Jackson.newObjectMapper();
  final Validator validator = Validators.newValidator();
  final YamlConfigurationFactory<NextGenConfiguration> factory =
      new YamlConfigurationFactory<>(NextGenConfiguration.class, validator, objectMapper, "dw");

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void test() throws Exception {
    final File yaml = new File(Resources.getResource("grpc-server-config.yml").toURI());
    final NextGenConfiguration configuration = factory.build(yaml);

    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      public GrpcServerConfig getGrpcServerConfig() {
        return configuration.getGrpcServerConfig();
      }
    });
    modules.add(new NgManagerGrpcServerModule(
        configuration.getGrpcServerConfig(), configuration.getNextGenConfig().getManagerServiceSecret()));
    Injector injector = Guice.createInjector(modules);
    injector.getInstance(ServiceManager.class).startAsync().awaitHealthy();
    NgDelegateTaskResponseGrpcServer ngDelegateTaskResponseGrpcServer =
        injector.getInstance(NgDelegateTaskResponseGrpcServer.class);
    assertThat(ngDelegateTaskResponseGrpcServer).isNotNull();
  }
}