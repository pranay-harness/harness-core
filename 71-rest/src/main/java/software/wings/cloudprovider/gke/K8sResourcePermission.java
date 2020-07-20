package software.wings.cloudprovider.gke;

import io.kubernetes.client.openapi.apis.AuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1ResourceAttributes;
import io.kubernetes.client.openapi.models.V1SubjectAccessReviewStatus;

import java.util.List;

public interface K8sResourcePermission {
  String DENIED_RESPOSE_FORMAT = "%s not granted on %s.%s, ";
  String FAILED_RESPOSE_FORMAT = "Failed to validate %s on %s.%s, ";

  V1SubjectAccessReviewStatus validate(AuthorizationV1Api apiClient, String group, String verb, String resource);

  V1SubjectAccessReviewStatus validate(AuthorizationV1Api apiClient, V1ResourceAttributes v1ResourceAttributes);

  List<V1SubjectAccessReviewStatus> validate(
      AuthorizationV1Api apiClient, List<V1ResourceAttributes> v1ResourceAttributesList, int timeoutsec);
}
