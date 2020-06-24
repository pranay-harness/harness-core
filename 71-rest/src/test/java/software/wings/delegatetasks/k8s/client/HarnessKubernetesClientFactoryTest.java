package software.wings.delegatetasks.k8s.client;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesConfig;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.service.impl.KubernetesHelperService;

import java.util.Collections;

public class HarnessKubernetesClientFactoryTest extends WingsBaseTest {
  @InjectMocks @Inject private HarnessKubernetesClientFactory harnessKubernetesClientFactory;
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private KubernetesHelperService kubernetesHelperService;

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testMasterURLModifier() {
    String masterURL = "https://35.197.55.65";
    String modifiedURL = harnessKubernetesClientFactory.modifyMasterUrl(masterURL);
    assertThat(modifiedURL).isEqualTo(masterURL + ":443");

    masterURL = "https://35.197.55.65:90";
    modifiedURL = harnessKubernetesClientFactory.modifyMasterUrl(masterURL);
    assertThat(modifiedURL).isEqualTo(masterURL);

    masterURL = "http://35.197.55.65";
    modifiedURL = harnessKubernetesClientFactory.modifyMasterUrl(masterURL);
    assertThat(modifiedURL).isEqualTo(masterURL + ":80");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testMasterURLModifierException() {
    String masterURL = "invalidURL";
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> harnessKubernetesClientFactory.modifyMasterUrl(masterURL));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldNotAddOrRemoveTrailingSlash() throws Exception {
    assertThat(harnessKubernetesClientFactory.modifyMasterUrl("https://test-domain.net"))
        .isEqualTo("https://test-domain.net:443");
    assertThat(harnessKubernetesClientFactory.modifyMasterUrl("https://test-domain.net/"))
        .isEqualTo("https://test-domain.net:443/");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPreservePathSegments() throws Exception {
    String masterUrl = "https://int-capi-rancher.cncpl.us/k8s/clusters/c-pv9p9";
    assertThat(harnessKubernetesClientFactory.modifyMasterUrl(masterUrl))
        .isEqualTo("https://int-capi-rancher.cncpl.us:443/k8s/clusters/c-pv9p9");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testNewKubernetesClient() {
    String masterUrl = "https://int-capi-rancher.cncpl.us/k8s/clusters/c-pv9p9";
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().masterUrl(masterUrl).build();
    K8sClusterConfig k8sClusterConfig = K8sClusterConfig.builder().build();
    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class)))
        .thenReturn(kubernetesConfig);

    harnessKubernetesClientFactory.newKubernetesClient(k8sClusterConfig);
    verify(kubernetesHelperService, times(1)).getKubernetesClient(kubernetesConfig, Collections.emptyList());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(k8sClusterConfig);
    assertThat(kubernetesConfig.getMasterUrl()).isEqualTo("https://int-capi-rancher.cncpl.us:443/k8s/clusters/c-pv9p9");
  }
}
