/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s;

import io.harness.k8s.model.response.CEK8sDelegatePrerequisite;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.ApisApi;
import io.kubernetes.client.openapi.apis.AuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1ResourceAttributes;
import io.kubernetes.client.openapi.models.V1ResourceAttributesBuilder;
import io.kubernetes.client.openapi.models.V1SelfSubjectAccessReviewBuilder;
import io.kubernetes.client.openapi.models.V1SubjectAccessReviewStatus;
import io.kubernetes.client.openapi.models.V1SubjectAccessReviewStatusBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class K8sResourceValidatorImpl implements K8sResourceValidator {
  @Inject @Named("asyncExecutor") ExecutorService executorService;

  @Override
  public boolean validateMetricsServer(ApiClient apiClient) throws ApiException {
    ApisApi apisApi = new ApisApi(apiClient);
    return apisApi.getAPIVersions().getGroups().stream().anyMatch(x -> "metrics.k8s.io".equals(x.getName()));
  }

  @Override
  public V1SubjectAccessReviewStatus validate(AuthorizationV1Api apiClient, String group, String verb, String resource)
      throws ApiException {
    return this.validate(
        apiClient, new V1ResourceAttributesBuilder().withGroup(group).withVerb(verb).withResource(resource).build());
  }

  @Override
  public V1SubjectAccessReviewStatus validate(AuthorizationV1Api apiClient, V1ResourceAttributes v1ResourceAttributes)
      throws ApiException {
    return apiClient
        .createSelfSubjectAccessReview(new V1SelfSubjectAccessReviewBuilder()
                                           .withNewSpec()
                                           .withResourceAttributes(v1ResourceAttributes)
                                           .endSpec()
                                           .build(),
            null, null, null)
        .getStatus();
  }

  @Override
  public List<V1SubjectAccessReviewStatus> validate(
      AuthorizationV1Api apiClient, List<V1ResourceAttributes> v1ResourceAttributesList, int timeoutsec) {
    List<Callable<V1SubjectAccessReviewStatus>> tasks = new ArrayList<>();
    v1ResourceAttributesList.forEach(
        v1ResourceAttributes -> tasks.add(() -> this.validate(apiClient, v1ResourceAttributes)));

    try {
      return executorService.invokeAll(tasks, timeoutsec, TimeUnit.SECONDS)
          .stream()
          .map(x -> {
            try {
              return x.get();
            } catch (Exception ex) {
              String message = "BACKEND_ERROR";
              if (ex.getMessage() != null && ex.getMessage().contains("ApiException")) {
                message = "ApiException";
              }
              return new V1SubjectAccessReviewStatusBuilder()
                  .withAllowed(false)
                  .withDenied(true)
                  .withEvaluationError(message)
                  .build();
            }
          })
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Failed to validate all permissions ", e);
      return new ArrayList<>();
    }
  }

  public static String buildResponse(
      List<V1ResourceAttributes> cePermissions, List<V1SubjectAccessReviewStatus> statuses) {
    StringBuilder st = new StringBuilder();

    IntStream.range(0, statuses.size()).forEach(i -> {
      if (null == statuses.get(i)) {
        st.append(String.format(FAILED_RESPOSE_FORMAT, cePermissions.get(i).getVerb(),
            cePermissions.get(i).getResource(), cePermissions.get(i).getGroup()));
      } else if (!statuses.get(i).getAllowed()) {
        st.append(String.format(DENIED_RESPOSE_FORMAT, cePermissions.get(i).getVerb(),
            cePermissions.get(i).getResource(), cePermissions.get(i).getGroup()));
      }
    });

    return st.toString();
  }

  public static List<V1ResourceAttributes> v1ResourceAttributesListBuilder(
      String[] apiGroups, String[] resources, String[] verbs) {
    List<V1ResourceAttributes> list = new ArrayList<>();
    for (String group : apiGroups) {
      for (String resource : resources) {
        for (String verb : verbs) {
          list.add(new V1ResourceAttributes().group(group).resource(resource).verb(verb));
        }
      }
    }
    return list;
  }

  private static List<V1ResourceAttributes> getAllCEPermissionsRequired() {
    List<V1ResourceAttributes> cePermissions = new ArrayList<>();

    cePermissions.addAll(ImmutableList.copyOf(v1ResourceAttributesListBuilder(new String[] {""},
        new String[] {"pods", "nodes", "events", "namespaces"}, new String[] {"get", "list", "watch"})));

    cePermissions.addAll(ImmutableList.copyOf(v1ResourceAttributesListBuilder(new String[] {"apps", "extensions"},
        new String[] {"statefulsets", "deployments", "daemonsets", "replicasets"},
        new String[] {"get", "list", "watch"})));

    cePermissions.addAll(ImmutableList.copyOf(v1ResourceAttributesListBuilder(
        new String[] {"batch"}, new String[] {"jobs", "cronjobs"}, new String[] {"get", "list", "watch"})));

    cePermissions.addAll(ImmutableList.copyOf(v1ResourceAttributesListBuilder(
        new String[] {"metrics.k8s.io"}, new String[] {"pods", "nodes"}, new String[] {"get", "list"})));

    return cePermissions;
  }

  public String validateCEPermissions(ApiClient apiClient) {
    AuthorizationV1Api authorizationV1Api = new AuthorizationV1Api(apiClient);
    final List<V1ResourceAttributes> cePermissions = getAllCEPermissionsRequired();

    List<V1SubjectAccessReviewStatus> response = this.validate(authorizationV1Api, cePermissions, 10);

    return buildResponse(cePermissions, response);
  }

  @Override
  public List<CEK8sDelegatePrerequisite.Rule> validateCEPermissions2(ApiClient apiClient) {
    AuthorizationV1Api authorizationV1Api = new AuthorizationV1Api(apiClient);
    final List<V1ResourceAttributes> cePermissions = getAllCEPermissionsRequired();

    List<CEK8sDelegatePrerequisite.Rule> ruleList = new ArrayList<>();

    List<V1SubjectAccessReviewStatus> statuses = this.validate(authorizationV1Api, cePermissions, 10);

    for (int i = 0; i < statuses.size(); i++) {
      V1ResourceAttributes resourceAttributes = cePermissions.get(i);
      String message = null;
      if (null == statuses.get(i)) {
        message = "BACKEND_ERROR";
      } else if (!statuses.get(i).getAllowed()) {
        if (statuses.get(i).getReason() != null) {
          message = statuses.get(i).getReason();
        } else if (statuses.get(i).getEvaluationError() != null) {
          message = statuses.get(i).getEvaluationError();
        }
      }
      if (message != null) {
        ruleList.add(CEK8sDelegatePrerequisite.Rule.builder()
                         .apiGroups(resourceAttributes.getGroup())
                         .resources(resourceAttributes.getResource())
                         .verbs(resourceAttributes.getVerb())
                         .message(message)
                         .build());
      }
    }

    return ruleList;
  }
}
