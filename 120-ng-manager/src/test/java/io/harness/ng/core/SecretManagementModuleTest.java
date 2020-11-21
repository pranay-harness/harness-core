package io.harness.ng.core;

import static io.harness.rule.OwnerRule.VIKAS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.entitysetupusageclient.EntitySetupUsageClientModule;
import io.harness.govern.ProviderModule;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.ng.core.api.NGSecretService;
import io.harness.ng.core.api.impl.NGSecretManagerServiceImpl;
import io.harness.ng.core.api.impl.NGSecretServiceImpl;
import io.harness.ng.core.api.repositories.spring.SecretRepository;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.SecretManagementClientModule;
import io.harness.secretmanagerclient.services.SecretManagerClientServiceImpl;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.NextGenRegistrars;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class SecretManagementModuleTest extends CategoryTest {
  private SecretManagementModule secretManagementModule;
  private SecretManagementClientModule secretManagementClientModule;
  private EntitySetupUsageClientModule entityReferenceClientModule;
  @Mock private SecretRepository secretRepository;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSecretManagementModule() {
    ServiceHttpClientConfig secretManagerClientConfig =
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:7143").build();
    ServiceHttpClientConfig ngManagerClientConfig =
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:3457").build();
    String serviceSecret = "test_secret";
    secretManagementModule = new SecretManagementModule();
    secretManagementClientModule =
        new SecretManagementClientModule(secretManagerClientConfig, serviceSecret, "NextGenManager");
    entityReferenceClientModule =
        new EntitySetupUsageClientModule(ngManagerClientConfig, serviceSecret, "NextGenManager");

    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> registrars() {
        return NextGenRegistrars.kryoRegistrars;
      }
    });
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      SecretRepository repository() {
        return mock(SecretRepository.class);
      }
    });
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      DelegateGrpcClientWrapper registerDelegateGrpcClientWrapper() {
        return mock(DelegateGrpcClientWrapper.class);
      }
    });
    modules.add(secretManagementModule);
    modules.add(secretManagementClientModule);
    modules.add(entityReferenceClientModule);
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
