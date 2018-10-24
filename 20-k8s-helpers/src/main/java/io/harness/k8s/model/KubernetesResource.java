package io.harness.k8s.model;

import lombok.Builder;
import lombok.Data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class KubernetesResource {
  private KubernetesResourceId resourceId;
  private Object value;

  private static final String dotMatch = "\\.";

  public Object getField(String key) {
    Object object = this.getValue();

    List<String> keyList = Arrays.asList(key.split(dotMatch));

    for (int i = 0; i < keyList.size() - 1; i++) {
      String item = keyList.get(i);
      if (object instanceof Map && ((Map) object).containsKey(item)) {
        object = ((Map) object).get(item);
      } else {
        return null;
      }
    }

    if (object instanceof Map) {
      return ((Map) object).get(keyList.get(keyList.size() - 1));
    }

    return null;
  }

  public KubernetesResource setField(String key, Object newValue) {
    if (this.getValue() == null) {
      this.setValue(new HashMap<>());
    }

    Map objectMap = (Map) this.getValue();

    List<String> keyList = Arrays.asList(key.split(dotMatch));

    for (int i = 0; i < keyList.size() - 1; i++) {
      String item = keyList.get(i);
      if (objectMap.containsKey(item)) {
        objectMap = (Map) objectMap.get(item);
      } else {
        objectMap.put(item, new HashMap<>());
      }
    }

    objectMap.put(keyList.get(keyList.size() - 1), newValue);

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
