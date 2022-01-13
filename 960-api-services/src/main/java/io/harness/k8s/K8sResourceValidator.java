/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.response.CEK8sDelegatePrerequisite;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1ResourceAttributes;
import io.kubernetes.client.openapi.models.V1SelfSubjectAccessReview;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.openapi.models.V1SubjectAccessReviewStatus;
import java.util.List;

@OwnedBy(HarnessTeam.CE)
public interface K8sResourceValidator {
  String DENIED_RESPOSE_FORMAT = "%s not granted on %s.%s, ";
  String FAILED_RESPOSE_FORMAT = "Failed to validate %s on %s.%s, ";

  V1SubjectAccessReviewStatus validate(AuthorizationV1Api apiClient, String group, String verb, String resource)
      throws ApiException;

  V1SubjectAccessReviewStatus validate(AuthorizationV1Api apiClient, V1ResourceAttributes v1ResourceAttributes)
      throws ApiException;

  List<V1SubjectAccessReviewStatus> validate(
      AuthorizationV1Api apiClient, List<V1ResourceAttributes> v1ResourceAttributesList, int timeoutsec);

  boolean validateMetricsServer(ApiClient apiClient) throws ApiException;

  List<CEK8sDelegatePrerequisite.Rule> validateCEPermissions2(ApiClient apiClient);

  List<V1Status> validateLightwingResourceExists(ApiClient apiClient) throws Exception;

  List<V1SelfSubjectAccessReview> validateLightwingResourcePermissions(ApiClient apiClient) throws ApiException;
}
