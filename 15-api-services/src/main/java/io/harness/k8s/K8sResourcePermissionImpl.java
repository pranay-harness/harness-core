package io.harness.k8s;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1ResourceAttributes;
import io.kubernetes.client.openapi.models.V1ResourceAttributesBuilder;
import io.kubernetes.client.openapi.models.V1SelfSubjectAccessReviewBuilder;
import io.kubernetes.client.openapi.models.V1SubjectAccessReviewStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Singleton
@Slf4j
public class K8sResourcePermissionImpl implements K8sResourcePermission {
  @Inject @Named("asyncExecutor") ExecutorService executorService;

  @Override
  public V1SubjectAccessReviewStatus validate(
      AuthorizationV1Api apiClient, String group, String verb, String resource) {
    return this.validate(
        apiClient, new V1ResourceAttributesBuilder().withGroup(group).withVerb(verb).withResource(resource).build());
  }

  @Override
  public V1SubjectAccessReviewStatus validate(AuthorizationV1Api apiClient, V1ResourceAttributes v1ResourceAttributes) {
    try {
      return apiClient
          .createSelfSubjectAccessReview(new V1SelfSubjectAccessReviewBuilder()
                                             .withNewSpec()
                                             .withResourceAttributes(v1ResourceAttributes)
                                             .endSpec()
                                             .build(),
              null, null, null)
          .getStatus();
    } catch (ApiException e) {
      return null;
    }
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
              return null;
            }
          })
          .collect(Collectors.toList());
    } catch (Exception e) {
      logger.error("Failed to validate all permissions ", e);
      return new ArrayList<>();
    }
  }

  public String buildResponse(List<V1ResourceAttributes> cePermissions, List<V1SubjectAccessReviewStatus> statuses) {
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

  public List<V1ResourceAttributes> v1ResourceAttributesListBuilder(
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
}
