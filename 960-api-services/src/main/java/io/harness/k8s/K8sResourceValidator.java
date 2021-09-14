/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s;

import io.harness.k8s.model.response.CEK8sDelegatePrerequisite;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1ResourceAttributes;
import io.kubernetes.client.openapi.models.V1SubjectAccessReviewStatus;
import java.util.List;

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
}
