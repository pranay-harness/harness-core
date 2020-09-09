package io.harness.k8s;

import static io.harness.k8s.KubernetesContainerServiceImpl.METRICS_SERVER_ABSENT;
import static io.harness.rule.OwnerRule.ANSHUL;
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
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.apiclient.ApiClientFactoryImpl;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.rule.Owner;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
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
  @Mock private K8sResourceValidatorImpl k8sResourceValidator;

  @InjectMocks @Spy private KubernetesContainerServiceImpl kubernetesContainerService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public KubernetesServer server = new KubernetesServer(true, true);

  private KubernetesClient client;
  private ApiClient apiClient;

  List<V1ResourceAttributes> resourceList;
  List<V1SubjectAccessReviewStatus> response;

  private final String RESULT = "watch not granted on pods.apps, ";

  @Before
  public void setUp() {
    client = server.getClient();
    when(kubernetesHelperService.getKubernetesClient(KUBERNETES_CONFIG)).thenReturn(client);
    apiClient = ApiClientFactoryImpl.fromKubernetesConfig(KUBERNETES_CONFIG, null);

    resourceList = Arrays.asList(new V1ResourceAttributes().verb("verb").resource("resource").group("group"));
    response = Arrays.asList(new V1SubjectAccessReviewStatus());
    doReturn(resourceList).when(k8sResourceValidator).v1ResourceAttributesListBuilder(any(), any(), any());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldValidateWithCEMetricsServerAbsent() throws ApiException {
    doReturn(false).when(k8sResourceValidator).validateMetricsServer(any(ApiClient.class));

    assertThatThrownBy(() -> kubernetesContainerService.validateCEPermissions(KUBERNETES_CONFIG))
        .isExactlyInstanceOf(InvalidRequestException.class)
        .hasMessage(METRICS_SERVER_ABSENT);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldValidateWithCEMetricsServerPresent() throws ApiException {
    doReturn(true).when(k8sResourceValidator).validateMetricsServer(any(ApiClient.class));
    doNothing().when(kubernetesContainerService).validateCEResourcePermission(any());

    assertThatCode(() -> kubernetesContainerService.validateCEPermissions(KUBERNETES_CONFIG))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldValidateWithCEAllPermissionGranted() throws ApiException {
    doReturn(true).when(k8sResourceValidator).validateMetricsServer(any(ApiClient.class));
    doReturn("").when(k8sResourceValidator).validateCEPermissions(any(ApiClient.class));

    assertThatCode(() -> kubernetesContainerService.validateCEResourcePermission(apiClient)).doesNotThrowAnyException();

    verify(k8sResourceValidator, times(1)).validateCEPermissions(any(ApiClient.class));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldValidateWithCEOnePermissionNotGranted() throws ApiException {
    doReturn(true).when(k8sResourceValidator).validateMetricsServer(any(ApiClient.class));
    doReturn(RESULT).when(k8sResourceValidator).validateCEPermissions(any(ApiClient.class));

    assertThatThrownBy(() -> kubernetesContainerService.validateCEResourcePermission(any(ApiClient.class)))
        .hasMessageContaining(RESULT);

    verify(k8sResourceValidator, times(1)).validateCEPermissions(any(ApiClient.class));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void shouldValidate() {
    assertThatCode(() -> kubernetesContainerService.validate(KUBERNETES_CONFIG)).doesNotThrowAnyException();
  }
}
