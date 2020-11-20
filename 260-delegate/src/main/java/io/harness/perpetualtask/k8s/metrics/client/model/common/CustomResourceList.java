package io.harness.perpetualtask.k8s.metrics.client.model.common;

import com.google.gson.annotations.SerializedName;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.k8s.model.KubernetesListObject;
import io.harness.k8s.model.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ListMeta;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
@JsonDeserialize(using = JsonDeserializer.None.class)
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
