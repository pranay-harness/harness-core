package io.harness.ng.core;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.services.api.NGSecretManagerService;
import io.harness.ng.core.services.api.NGSecretService;
import io.harness.ng.core.services.api.NgSecretUsageService;
import io.harness.ng.core.services.api.impl.NGSecretManagerServiceImpl;
import io.harness.ng.core.services.api.impl.NGSecretServiceImpl;
import io.harness.ng.core.services.api.impl.NGSecretUsageServiceImpl;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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

    NGSecretManagerService ngSecretManagerService = injector.getInstance(NGSecretManagerService.class);
    assertThat(ngSecretManagerService).isNotNull();
    assertThat(ngSecretManagerService).isInstanceOf(NGSecretManagerServiceImpl.class);

    NGSecretService ngSecretService = injector.getInstance(NGSecretService.class);
    assertThat(ngSecretService).isNotNull();
    assertThat(ngSecretService).isInstanceOf(NGSecretServiceImpl.class);

    NgSecretUsageService ngSecretUsageService = injector.getInstance(NgSecretUsageService.class);
    assertThat(ngSecretUsageService).isNotNull();
    assertThat(ngSecretUsageService).isInstanceOf(NGSecretUsageServiceImpl.class);
  }
}
