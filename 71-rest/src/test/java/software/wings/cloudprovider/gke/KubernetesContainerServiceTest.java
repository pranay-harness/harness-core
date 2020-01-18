package software.wings.cloudprovider.gke;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.TimeLimiter;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.KubernetesConfig;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.service.impl.KubernetesHelperService;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

import java.time.Clock;
import java.util.Collections;

public class KubernetesContainerServiceTest extends CategoryTest {
  private static final KubernetesConfig KUBERNETES_CONFIG = KubernetesConfig.builder().namespace("default").build();

  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private TimeLimiter timeLimiter;
  @Mock private Clock clock;
  @Mock private K8sGlobalConfigService k8sGlobalConfigService;

  @InjectMocks private KubernetesContainerServiceImpl kubernetesContainerService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public KubernetesServer server = new KubernetesServer(true, true);

  private KubernetesClient client;
  private String kubectlPath = "/kubectlPath";
  private String kubeconfigFileContent = "";

  @Before
  public void setUp() throws Exception {
    client = server.getClient();
    when(kubernetesHelperService.getKubernetesClient(KUBERNETES_CONFIG, Collections.emptyList())).thenReturn(client);
    when(k8sGlobalConfigService.getKubectlPath()).thenReturn(kubectlPath);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldValidateWithoutCE() {
    assertThatCode(() -> kubernetesContainerService.validate(KUBERNETES_CONFIG, Collections.emptyList(), false))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldValidateWithCE() {
    when(containerDeploymentDelegateHelper.getConfigFileContent(KUBERNETES_CONFIG)).thenReturn(kubeconfigFileContent);
    assertThatThrownBy(() -> kubernetesContainerService.validate(KUBERNETES_CONFIG, Collections.emptyList(), true))
        .isInstanceOf(InvalidRequestException.class);
  }
}
