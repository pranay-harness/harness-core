/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.watch;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Suppliers;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.apis.ApiextensionsV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1CustomResourceDefinition;
import io.kubernetes.client.openapi.models.V1CustomResourceDefinitionList;
import io.kubernetes.client.openapi.models.V1CustomResourceDefinitionNames;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Used for getting details of CRD workload types that we don't watch.
 */
@OwnedBy(HarnessTeam.CE)
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class CrdWorkloadFetcher {
  private volatile boolean tripped;

  @Value
  @Builder
  public static class WorkloadReference {
    String namespace;
    String name;
    String kind;
    String apiVersion;
    String uid;
  }

  @Value
  public static class HavingMetadataObject {
    V1ObjectMeta metadata;
  }

  private final ApiClient apiClient;
  private final LoadingCache<WorkloadReference, Workload> workloadCache;
  private final Supplier<Map<String, String>> kindToPluralMapSupplier;

  public CrdWorkloadFetcher(ApiClient apiClient) {
    this.tripped = false;
    this.apiClient = apiClient;
    workloadCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(this::getWorkloadInternal);
    kindToPluralMapSupplier = Suppliers.memoizeWithExpiration(this::loadKindToPluralMap, 10, TimeUnit.MINUTES);
  }

  private Map<String, String> loadKindToPluralMap() {
    Map<String, String> plurals = new HashMap<>();
    ApiextensionsV1Api apiextensionsV1Api = new ApiextensionsV1Api(apiClient);
    try {
      V1CustomResourceDefinitionList v1CustomResourceDefinitionList =
          apiextensionsV1Api.listCustomResourceDefinition(null, null, null, null, null, null, null, null, null);
      for (V1CustomResourceDefinition crd : v1CustomResourceDefinitionList.getItems()) {
        V1CustomResourceDefinitionNames names = crd.getSpec().getNames();
        plurals.put(names.getKind(), names.getPlural());
      }

    } catch (ApiException e) {
      log.warn("Encountered ApiException listing CRDs, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
    } catch (Exception e) {
      log.warn("Error listing CRDs", e);
    }
    return plurals;
  }

  private String getPluralForKind(String kind) {
    return Optional
        .ofNullable(kindToPluralMapSupplier.get().get(kind))
        // fallback (works for regular nouns)
        .orElseGet(() -> {
          String singular = kind.toLowerCase();
          return singular.endsWith("s") ? singular + "es" : singular + "s";
        });
  }

  public Workload getWorkload(WorkloadReference workloadReference) {
    return workloadCache.get(workloadReference);
  }

  private Workload getWorkloadInternal(WorkloadReference workloadRef) {
    // Try getting the details of the workload using the generic api
    //    Handle permission missing case - log a warning and have same behavior as current
    V1ObjectMeta knownMetadata = new V1ObjectMetaBuilder()
                                     .withName(workloadRef.getName())
                                     .withNamespace(workloadRef.getNamespace())
                                     .withUid(workloadRef.getUid())
                                     .build();
    // there is no definite spec.schema for CRD, defaulting to 1.
    // Ref:
    // https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/#create-a-customresourcedefinition
    Integer replicas = 1;
    if (tripped) {
      return Workload.of(workloadRef.getKind(), knownMetadata, replicas);
    }
    try {
      String apiVersion = workloadRef.getApiVersion();
      String[] parts = apiVersion.split("/");
      String group;
      String version;
      if (parts.length == 1) {
        group = "core";
        version = parts[0];
      } else {
        group = parts[0];
        version = parts[1];
      }
      CustomObjectsApi api = new CustomObjectsApi(apiClient);
      String pluralName = getPluralForKind(workloadRef.getKind());
      Object workloadObject =
          api.getNamespacedCustomObject(group, version, workloadRef.getNamespace(), pluralName, workloadRef.getName());
      JSON json = apiClient.getJSON();
      HavingMetadataObject havingMetadataObject =
          json.deserialize(json.serialize(workloadObject), HavingMetadataObject.class);
      return Workload.of(workloadRef.getKind(),
          Optional.ofNullable(havingMetadataObject.getMetadata()).orElse(knownMetadata), replicas);
    } catch (ApiException e) {
      log.warn(
          "Encountered ApiException fetching custom workload, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
      if (e.getCode() == 400 || e.getCode() == 401 || e.getCode() == 403 || e.getCode() == 404) {
        log.warn("Tripping future calls. Response code is {}", e.getCode());
        tripped = true;
      }
      // fallback to return a workload with the metadata we already know (without others like labels)
      return Workload.of(workloadRef.getKind(), knownMetadata, replicas);
    } catch (Exception e) {
      log.warn("Encountered error trying to fetch custom workload details", e);
      // fallback to return a workload with the metadata we already know (without others like labels)
      return Workload.of(workloadRef.getKind(), knownMetadata, replicas);
    }
  }
}
