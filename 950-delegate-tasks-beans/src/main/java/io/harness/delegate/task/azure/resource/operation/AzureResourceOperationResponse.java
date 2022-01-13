/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.azure.resource.operation;

import io.harness.delegate.task.azure.resource.operation.acr.ACRListRepositoryTagsOperationResponse;
import io.harness.delegate.task.azure.resource.operation.k8s.AzureListKubernetesClustersOperationResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonSubTypes({
  @JsonSubTypes.Type(
      value = AzureListKubernetesClustersOperationResponse.class, name = "azureKubernetesClustersListResourceOperation")
  ,
      @JsonSubTypes.Type(
          value = ACRListRepositoryTagsOperationResponse.class, name = "acrListRepositoryTagsOperationResponse")
})

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface AzureResourceOperationResponse {}
