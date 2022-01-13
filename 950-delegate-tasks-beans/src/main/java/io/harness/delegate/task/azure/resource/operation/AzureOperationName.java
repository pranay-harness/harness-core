/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.azure.resource.operation;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AzureOperationName {
  LIST_ACR_REPOSITORY_TAGS("ListACRRepositoryTags"),
  LIST_KUBERNETES_CLUSTERS("ListKubernetesClusters");

  private final String value;

  AzureOperationName(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
