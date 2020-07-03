package io.harness.ng.core;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.services.api.impl.NGSecretManagerImpl;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.service.intfc.security.SecretManager;

import java.util.ArrayList;
import java.util.List;

public class SecretManagementModuleTest extends BaseTest {
  private SecretManagementModule secretManagementModule;

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSecretManagementModule() {
    SecretManagerClientConfig secretManagerClientConfig =
        SecretManagerClientConfig.builder().baseUrl("http://localhost:7143").build();
    String serviceSecret = "test_secret";
    secretManagementModule = new SecretManagementModule(secretManagerClientConfig, serviceSecret);

    List<Module> modules = new ArrayList<>();
    modules.add(secretManagementModule);
    Injector injector = Guice.createInjector(modules);

    SecretManager secretManager = injector.getInstance(SecretManager.class);
    assertThat(secretManager).isNotNull();
    assertThat(secretManager instanceof NGSecretManagerImpl).isTrue();
  }
}
