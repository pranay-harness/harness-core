package io.harness.perpetualtask.k8s.metrics.client.model.common;

import com.google.gson.annotations.SerializedName;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.k8s.model.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@JsonDeserialize(using = JsonDeserializer.None.class)
public abstract class CustomResource implements KubernetesObject {
  @SerializedName("kind") private String kind;
  @SerializedName("apiVersion") private String apiVersion;
  @SerializedName("metadata") private V1ObjectMeta metadata = new V1ObjectMeta();

  public CustomResource() {
    this.kind = this.getClass().getSimpleName();
  }

  public CustomResource(String kind) {
    this.kind = kind;
  }

  public String toString() {
    return "K8sCustomResource{kind='" + this.kind + '\'' + ", apiVersion='" + this.apiVersion + '\''
        + ", v1ObjectMeta=" + this.metadata + '}';
  }

  public String getKind() {
    return this.kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getApiVersion() {
    return this.apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public V1ObjectMeta getMetadata() {
    return this.metadata;
  }

  public void setMetadata(V1ObjectMeta metadata) {
    this.metadata = metadata;
  }
}
