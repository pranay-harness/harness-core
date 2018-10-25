package io.harness.k8s.model;

import io.harness.exception.InvalidArgumentsException;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

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

  private static boolean isCollection(String str) {
    return str.endsWith("]");
  }

  private static String getCollectionName(String str) {
    return str.split("\\[")[0];
  }

  private static int getIndex(String str) {
    // assumes index is valid in format like: containers[1]
    String num = str.split("\\[")[1].split("]")[0];
    return Integer.parseInt(num);
  }

  public Object getField(String key) {
    Object object = this.getValue();

    List<String> keyList = Arrays.asList(key.split(dotMatch));

    for (int i = 0; i < keyList.size(); i++) {
      String item = keyList.get(i);

      if (isCollection(item)) {
        String collectionName = getCollectionName(item);
        object = ((Map) object).get(collectionName);
        if (object instanceof List) {
          int index = getIndex(item);
          object = ((List) object).get(index);
        } else {
          return null;
        }
      } else {
        if (object instanceof Map && ((Map) object).containsKey(item)) {
          object = ((Map) object).get(item);
        } else {
          return null;
        }
      }
    }

    return object;
  }

  public KubernetesResource setField(String key, Object newValue) {
    if (this.getValue() == null) {
      this.setValue(new HashMap<>());
    }

    Map objectMap = (Map) this.getValue();

    List<String> keyList = Arrays.asList(key.split(dotMatch));

    for (int i = 0; i < keyList.size() - 1; i++) {
      String item = keyList.get(i);
      if (isCollection(item)) {
        String collectionName = getCollectionName(item);
        Object object = objectMap.get(collectionName);
        if (object instanceof List) {
          int index = getIndex(item);
          objectMap = (Map) ((List) object).get(index);
        } else {
          throw new InvalidArgumentsException(Pair.of(key, "collection not found"));
        }
      } else {
        if (objectMap.containsKey(item)) {
          objectMap = (Map) objectMap.get(item);
        } else {
          objectMap.put(item, new HashMap<>());
        }
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
