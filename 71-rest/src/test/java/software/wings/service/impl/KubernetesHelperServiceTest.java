package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.utils.WingsTestConstants.PASSWORD;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.openshift.client.OpenShiftClient;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import okhttp3.OkHttpClient;
import org.junit.Before;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesClusterAuthType;
import software.wings.beans.KubernetesConfig;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.service.intfc.security.EncryptionService;

import java.util.Collections;

public class KubernetesHelperServiceTest extends WingsBaseTest {
  public static final String MASTER_URL = "http://masterUrl/";
  public static final String OC_URL = "http://masterUrl/oapi/v1/";
  public static final String USERNAME = "username";

  @Mock private OkHttpClient okHttpClient;
  @Mock private EncryptionService encryptionService;
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @InjectMocks private KubernetesHelperService helperService;
  @InjectMocks private KubernetesHelperService kubernetesHelperService = spy(KubernetesHelperService.class);

  @Before
  public void setup() {
    doReturn(new OkHttpClient()).when(kubernetesHelperService).createHttpClientWithProxySetting(any(Config.class));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetOpenShiftClient() {
    ocClientWithNullNamespace();
    ocClientWithDefaultNamespace();
    ocClientWithNonDefaultNamespace();
  }

  private void ocClientWithNullNamespace() {
    final String namespace = null;
    OpenShiftClient ocClient =
        kubernetesHelperService.getOpenShiftClient(configWithNameSpace(namespace), Collections.emptyList());
    assertThat(ocClient.getOpenshiftUrl().toString()).isEqualTo(OC_URL);
    assertThat(ocClient.getMasterUrl().toString()).isEqualTo(MASTER_URL);
  }

  private void ocClientWithDefaultNamespace() {
    final String namespace = "default";
    OpenShiftClient ocClient =
        kubernetesHelperService.getOpenShiftClient(configWithNameSpace(namespace), Collections.emptyList());
    assertThat(ocClient.getNamespace()).isEqualTo(namespace);
    assertThat(ocClient.getOpenshiftUrl().toString()).isEqualTo(OC_URL);
    assertThat(ocClient.getMasterUrl().toString()).isEqualTo(MASTER_URL);
  }

  private void ocClientWithNonDefaultNamespace() {
    final String namespace = "newSpace";
    OpenShiftClient ocClient =
        kubernetesHelperService.getOpenShiftClient(configWithNameSpace(namespace), Collections.emptyList());
    assertThat(ocClient.getNamespace()).isEqualTo(namespace);
    assertThat(ocClient.getOpenshiftUrl().toString()).isEqualTo(OC_URL);
    assertThat(ocClient.getMasterUrl().toString()).isEqualTo(MASTER_URL);
  }

  private KubernetesConfig configWithNameSpace(String namespace) {
    return KubernetesConfig.builder()
        .masterUrl(MASTER_URL)
        .namespace(namespace)
        .username(USERNAME)
        .password(PASSWORD)
        .build();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetKubernetesClient() {
    KubernetesConfig kubernetesConfig = new KubernetesConfig();
    kubernetesConfig.setAuthType(KubernetesClusterAuthType.OIDC);
    kubernetesConfig.setDecrypted(true);
    when(containerDeploymentDelegateHelper.getOidcIdToken(kubernetesConfig)).thenReturn(null);

    helperService.getKubernetesClient(kubernetesConfig, Collections.emptyList());
    verify(containerDeploymentDelegateHelper, times(1)).getOidcIdToken(kubernetesConfig);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetIstioClient() {
    KubernetesConfig kubernetesConfig = new KubernetesConfig();
    kubernetesConfig.setAuthType(KubernetesClusterAuthType.OIDC);
    kubernetesConfig.setDecrypted(true);
    when(containerDeploymentDelegateHelper.getOidcIdToken(kubernetesConfig)).thenReturn(null);

    helperService.getIstioClient(kubernetesConfig, Collections.emptyList());
    verify(containerDeploymentDelegateHelper, times(1)).getOidcIdToken(kubernetesConfig);
  }
}
