package io.harness.k8s;

import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.HANTANG;
import static io.harness.rule.OwnerRule.UTSAV;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.TimeLimiter;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.rule.Owner;
import io.kubernetes.client.openapi.apis.AuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1ResourceAttributes;
import io.kubernetes.client.openapi.models.V1SubjectAccessReviewStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

public class KubernetesContainerServiceTest extends CategoryTest {
  private static final KubernetesConfig KUBERNETES_CONFIG = KubernetesConfig.builder().namespace("default").build();

  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private TimeLimiter timeLimiter;
  @Mock private Clock clock;
  @Mock private K8sGlobalConfigService k8sGlobalConfigService;
  @Mock private AuthorizationV1Api apiClient;
  @Mock private K8sResourcePermissionImpl k8sResourcePermission;

  @InjectMocks @Spy private KubernetesContainerServiceImpl kubernetesContainerService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public KubernetesServer server = new KubernetesServer(true, true);

  private KubernetesClient client;
  List<V1ResourceAttributes> resourceList;
  List<V1SubjectAccessReviewStatus> response;

  final String REASON = "REASON_HERE";

  @Before
  public void setUp() {
    client = server.getClient();
    when(kubernetesHelperService.getKubernetesClient(KUBERNETES_CONFIG)).thenReturn(client);

    resourceList = Arrays.asList(new V1ResourceAttributes().verb("verb").resource("resource").group("group"));
    response = Arrays.asList(new V1SubjectAccessReviewStatus());
    doReturn(resourceList).when(k8sResourcePermission).v1ResourceAttributesListBuilder(any(), any(), any());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldValidateWithAndWithoutCE() {
    assertThatCode(() -> kubernetesContainerService.validate(KUBERNETES_CONFIG)).doesNotThrowAnyException();

    doNothing().when(kubernetesContainerService).validateCEPermissions(any());
    assertThatCode(() -> kubernetesContainerService.validate(KUBERNETES_CONFIG)).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldValidateWithCEAllPermissionGranted() {
    response.get(0).allowed(true).reason(null);

    doReturn(response)
        .when(k8sResourcePermission)
        .validate(any(AuthorizationV1Api.class), any(List.class), any(int.class));
    doReturn("").when(k8sResourcePermission).buildResponse(any(), any());

    assertThatCode(() -> kubernetesContainerService.validateCEPermissions(KUBERNETES_CONFIG))
        .doesNotThrowAnyException();
    verify(k8sResourcePermission, times(0)).validate(any(AuthorizationV1Api.class), any(V1ResourceAttributes.class));
    verify(k8sResourcePermission, times(0))
        .validate(any(AuthorizationV1Api.class), any(String.class), any(String.class), any(String.class));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldValidateWithCEOnePermissionNotGranted() {
    response.get(0).allowed(false).reason(REASON);

    doReturn(response)
        .when(k8sResourcePermission)
        .validate(any(AuthorizationV1Api.class), any(List.class), any(int.class));
    doReturn(REASON).when(k8sResourcePermission).buildResponse(any(), any());

    assertThatThrownBy(() -> kubernetesContainerService.validateCEPermissions(KUBERNETES_CONFIG))
        .hasMessageContaining(REASON);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void shouldValidate() {
    assertThatCode(() -> kubernetesContainerService.validate(KUBERNETES_CONFIG)).doesNotThrowAnyException();
  }
}
