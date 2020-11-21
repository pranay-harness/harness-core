package software.wings.delegatetasks.k8s.client;

import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.govern.ProviderModule;
import io.harness.k8s.KubernetesContainerService;
import io.harness.rule.Owner;

import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.service.intfc.security.EncryptionService;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Slf4j
@RunWith(JUnit4.class)
public class KubernetesClientFactoryModuleTest extends CategoryTest {
  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testKubernetesClientFactoryModule() {
    List<Module> modules = new ArrayList<>();
    modules.add(new KubernetesClientFactoryModule());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      GkeClusterService gkeClusterService() {
        return mock(GkeClusterService.class);
      }
    });

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      KubernetesContainerService kubernetesContainerService() {
        return mock(KubernetesContainerService.class);
      }
    });

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      EncryptionService encryptionService() {
        return mock(EncryptionService.class);
      }
    });

    Injector injector = Guice.createInjector(modules);

    KubernetesClientFactory k8sClientFactory = injector.getInstance(KubernetesClientFactory.class);
    assertThat(k8sClientFactory).isNotNull();
    assertThat(k8sClientFactory).isInstanceOf(HarnessKubernetesClientFactory.class);
  }
}
