/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.model;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import io.kubernetes.client.common.KubernetesType;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

/**
 * Common accessors for kubernetes object.
 */
@OwnedBy(CE)
public interface KubernetesObject extends KubernetesType {
  /**
   * Gets metadata.
   *
   * ObjectMeta is metadata that all persisted resources must have, which includes all objects
   * users must create.
   *
   * @return the metadata
   */
  V1ObjectMeta getMetadata();
}
