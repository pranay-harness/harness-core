/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
