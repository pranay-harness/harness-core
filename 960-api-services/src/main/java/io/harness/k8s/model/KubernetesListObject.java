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
import io.kubernetes.client.openapi.models.V1ListMeta;
import java.util.List;

/**
 * Common accessors for kubernetes list object.
 */
@OwnedBy(CE)
public interface KubernetesListObject extends KubernetesType {
  /**
   * Gets list metadata.
   *
   * ListMeta describes metadata that synthetic resources must have, including lists and
   * various status objects. A resource may have only one of {ObjectMeta, ListMeta}.
   *
   *
   * @return the metadata
   */
  V1ListMeta getMetadata();

  /**
   * Gets the object items.
   *
   * @return the items
   */
  List<? extends KubernetesObject> getItems();
}
