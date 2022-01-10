/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.metrics.client.model.common;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.k8s.model.KubernetesListObject;
import io.harness.k8s.model.KubernetesObject;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.openapi.models.V1ListMeta;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@JsonDeserialize(using = JsonDeserializer.None.class)
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class CustomResourceList<T extends KubernetesObject> implements KubernetesListObject {
  @SerializedName("apiVersion") private String apiVersion;
  @SerializedName("items") private List<T> items = new ArrayList();
  @SerializedName("kind") private String kind;
  @SerializedName("metadata") private V1ListMeta metadata;

  public String getApiVersion() {
    return this.apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public List<T> getItems() {
    return this.items;
  }

  public void setItems(List<T> items) {
    this.items = items;
  }

  public String getKind() {
    return this.kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public void setMetadata(V1ListMeta metadata) {
    this.metadata = metadata;
  }

  /**
   * Gets metadata.
   * <p>
   * ObjectMeta is metadata that all persisted resources must have, which includes all objects
   * users must create.
   *
   * @return the metadata
   */
  @Override
  public V1ListMeta getMetadata() {
    return this.metadata;
  }
}
