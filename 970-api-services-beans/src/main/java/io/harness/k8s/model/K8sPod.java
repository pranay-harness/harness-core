/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sPod {
  private String uid;
  private String name;
  private String namespace;
  private String releaseName;
  private String podIP;
  private List<K8sContainer> containerList;
  private boolean newPod;
  private Map<String, String> labels;

  @JsonIgnore
  public String getColor() {
    if (labels != null) {
      return labels.get(HarnessLabels.color);
    }

    return null;
  }
}
