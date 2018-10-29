package io.harness.k8s.model;

import io.harness.k8s.manifest.ObjectYamlUtils;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

@Data
@Builder
public class KubernetesResource {
  private KubernetesResourceId resourceId;
  private Object value;

  private static final String dotMatch = "\\.";

  public Object getField(String key) {
    return ObjectYamlUtils.getField(this.getValue(), key);
  }

  public List<Object> getFields(String key) {
    return ObjectYamlUtils.getFields(this.getValue(), key);
  }

  public KubernetesResource setField(String key, Object newValue) {
    ObjectYamlUtils.setField(this.getValue(), key, newValue);
    return this;
  }

  public KubernetesResource transformField(String key, UnaryOperator<Object> transformer) {
    ObjectYamlUtils.transformField(this.getValue(), key, transformer);
    return this;
  }

  public KubernetesResource addAnnotations(Map newAnnotations) {
    Map annotations = (Map) this.getField("metadata.annotations");
    if (annotations == null) {
      annotations = new HashMap();
    }

    annotations.putAll(newAnnotations);
    return this.setField("metadata.annotations", annotations);
  }

  public KubernetesResource addLabels(Map newLabels) {
    Map labels = (Map) this.getField("metadata.labels");
    if (labels == null) {
      labels = new HashMap();
    }

    labels.putAll(newLabels);
    return this.setField("metadata.labels", labels);
  }
}
