package io.harness.ng.core;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.category.element.UnitTests;
import io.harness.govern.ProviderModule;
import io.harness.ng.core.services.api.NGSecretManagerService;
import io.harness.ng.core.services.api.NGSecretService;
import io.harness.ng.core.services.api.impl.NGSecretManagerServiceImpl;
import io.harness.ng.core.services.api.impl.NGSecretServiceImpl;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.SecretManagementClientModule;
import io.harness.secretmanagerclient.SecretManagerClientConfig;
import io.harness.secretmanagerclient.services.SecretManagerClientServiceImpl;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.NextGenRegistrars;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SecretManagementModuleTest {
  private SecretManagementModule secretManagementModule;
  private SecretManagementClientModule secretManagementClientModule;

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSecretManagementModule() {
    SecretManagerClientConfig secretManagerClientConfig =
        SecretManagerClientConfig.builder().baseUrl("http://localhost:7143").build();
    String serviceSecret = "test_secret";
    secretManagementModule = new SecretManagementModule();
    secretManagementClientModule = new SecretManagementClientModule(secretManagerClientConfig, serviceSecret);

    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> registrars() {
        return NextGenRegistrars.kryoRegistrars;
      }
    });
    modules.add(secretManagementModule);
    modules.add(secretManagementClientModule);
    Injector injector = Guice.createInjector(modules);

    NGSecretManagerService ngSecretManagerService = injector.getInstance(NGSecretManagerService.class);
    assertThat(ngSecretManagerService).isNotNull();
    assertThat(ngSecretManagerService).isInstanceOf(NGSecretManagerServiceImpl.class);

    NGSecretService ngSecretService = injector.getInstance(NGSecretService.class);
    assertThat(ngSecretService).isNotNull();
    assertThat(ngSecretService).isInstanceOf(NGSecretServiceImpl.class);

    SecretManagerClientService secretManagerClientService = injector.getInstance(SecretManagerClientService.class);
    assertThat(secretManagerClientService).isNotNull();
    assertThat(secretManagerClientService).isInstanceOf(SecretManagerClientServiceImpl.class);
  }
}
