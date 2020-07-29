package software.wings.cloudprovider.gke;

import static io.harness.k8s.K8sResourcePermission.DENIED_RESPOSE_FORMAT;
import static io.harness.k8s.K8sResourcePermission.FAILED_RESPOSE_FORMAT;
import static io.harness.rule.OwnerRule.UTSAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.category.element.UnitTests;
import io.harness.k8s.K8sResourcePermissionImpl;
import io.harness.rule.Owner;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1ResourceAttributes;
import io.kubernetes.client.openapi.models.V1ResourceAttributesBuilder;
import io.kubernetes.client.openapi.models.V1SelfSubjectAccessReview;
import io.kubernetes.client.openapi.models.V1SelfSubjectAccessReviewSpec;
import io.kubernetes.client.openapi.models.V1SubjectAccessReviewStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class K8sResourcePermissionImplTest extends WingsBaseTest {
  @Mock AuthorizationV1Api apiClient;
  @Spy @Inject @Named("asyncExecutor") ExecutorService executorService;
  @InjectMocks K8sResourcePermissionImpl k8sResourcePermission;

  private V1SubjectAccessReviewStatus v1SubjectAccessReviewStatus;
  private V1ResourceAttributes v1ResourceAttributes;
  private V1SelfSubjectAccessReviewSpec v1SelfSubjectAccessReviewSpec;
  private V1SelfSubjectAccessReview v1SelfSubjectAccessReview;
  private List<V1ResourceAttributes> v1ResourceAttributesList;

  final String GROUP = "apps";
  final String VERB = "delete";
  final String RESOURCE = "pods";
  final String REASON = "REASON";

  @Before
  public void setup() throws ApiException {
    v1ResourceAttributes =
        new V1ResourceAttributesBuilder().withGroup(GROUP).withResource(RESOURCE).withVerb(VERB).build();

    v1SubjectAccessReviewStatus = new V1SubjectAccessReviewStatus().allowed(true);
    v1SelfSubjectAccessReviewSpec = new V1SelfSubjectAccessReviewSpec().resourceAttributes(v1ResourceAttributes);

    v1SelfSubjectAccessReview =
        new V1SelfSubjectAccessReview().spec(v1SelfSubjectAccessReviewSpec).status(v1SubjectAccessReviewStatus);

    v1ResourceAttributesList = Arrays.asList(v1ResourceAttributes);

    doReturn(v1SelfSubjectAccessReview)
        .when(apiClient)
        .createSelfSubjectAccessReview(any(V1SelfSubjectAccessReview.class), any(), any(), any());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldValidateWithStringInput() {
    V1SubjectAccessReviewStatus status = k8sResourcePermission.validate(apiClient, GROUP, VERB, RESOURCE);
    assertThat(status).isNotNull();
    assertThat(status.getAllowed()).isTrue();
    assertThat(status.getReason()).isNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldValidateWithV1ResourceAttributesPermissionGranted() {
    V1SubjectAccessReviewStatus status = k8sResourcePermission.validate(apiClient, v1ResourceAttributes);
    assertThat(status).isNotNull();
    assertThat(status.getAllowed()).isTrue();
    assertThat(status.getReason()).isNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldValidateWithV1ResourceAttributesPermissionNotGranted() {
    v1SubjectAccessReviewStatus = v1SubjectAccessReviewStatus.allowed(false).reason(REASON);

    V1SubjectAccessReviewStatus status = k8sResourcePermission.validate(apiClient, v1ResourceAttributes);
    assertThat(status).isNotNull();
    assertThat(status.getAllowed()).isFalse();
    assertThat(status.getReason()).isEqualTo(REASON);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldValidateWithListV1ResourceAttributesNotGranted() {
    v1SubjectAccessReviewStatus = v1SubjectAccessReviewStatus.allowed(false).reason(REASON);

    List<V1SubjectAccessReviewStatus> result = k8sResourcePermission.validate(apiClient, v1ResourceAttributesList, 10);

    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0).getAllowed()).isFalse();
    assertThat(result.get(0).getReason()).isEqualTo(REASON);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldValidateWithListV1ResourceAttributesGranted() {
    List<V1SubjectAccessReviewStatus> result = k8sResourcePermission.validate(apiClient, v1ResourceAttributesList, 10);

    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0).getAllowed()).isTrue();
    assertThat(result.get(0).getReason()).isNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void validateWithListV1ResourceAttributesShouldCompleteWithoutException() {
    k8sResourcePermission.validate(apiClient, v1ResourceAttributesList, -1);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldExtractResponseAsStringWhenAllPermissionGranted() {
    String response = k8sResourcePermission.buildResponse(
        Arrays.asList(v1ResourceAttributes), Arrays.asList(v1SubjectAccessReviewStatus));

    assertThat(response).isNotNull().isEmpty();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldBuildResponseAsStringWhenOnePermissionNotGranted() {
    v1SubjectAccessReviewStatus = v1SubjectAccessReviewStatus.allowed(false).reason(REASON);

    String response =
        k8sResourcePermission.buildResponse(v1ResourceAttributesList, Arrays.asList(v1SubjectAccessReviewStatus));

    assertThat(response).isNotNull().isEqualTo(String.format(DENIED_RESPOSE_FORMAT, v1ResourceAttributes.getVerb(),
        v1ResourceAttributes.getResource(), v1ResourceAttributes.getGroup()));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldBuildResponseAsStringWhenOnePermissionGranted() {
    String response = k8sResourcePermission.buildResponse(
        Arrays.asList(v1ResourceAttributes), Arrays.asList(v1SubjectAccessReviewStatus));

    assertThat(response).isNotNull().isEmpty();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldBuildv1ResourceAttributesList() {
    List<V1ResourceAttributes> list = k8sResourcePermission.v1ResourceAttributesListBuilder(
        new String[] {GROUP}, new String[] {RESOURCE, RESOURCE}, new String[] {VERB, VERB, VERB});

    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(6);
    assertThat(list.get(0)).isEqualTo(v1ResourceAttributes);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldHandleNullAsValidateError() throws ApiException {
    doThrow(new ApiException("")).when(apiClient).createSelfSubjectAccessReview(any(), any(), any(), any());
    List<V1SubjectAccessReviewStatus> statuses =
        k8sResourcePermission.validate(apiClient, v1ResourceAttributesList, 10);

    String response = k8sResourcePermission.buildResponse(Arrays.asList(v1ResourceAttributes), statuses);

    assertThat(statuses).isNotNull();
    assertThat(statuses.size()).isEqualTo(1);
    assertThat(response).isNotNull().isEqualTo(FAILED_RESPOSE_FORMAT, v1ResourceAttributes.getVerb(),
        v1ResourceAttributes.getResource(), v1ResourceAttributes.getGroup());
  }
}
