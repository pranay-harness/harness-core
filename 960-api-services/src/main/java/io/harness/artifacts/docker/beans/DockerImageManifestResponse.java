/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.artifacts.docker.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@OwnedBy(CDC)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerImageManifestResponse {
  private String name;
  private List<DockerImageManifestHistoryElement> history;

  @Data
  public static class DockerImageManifestHistoryElement {
    private String v1Compatibility;

    public Map<String, String> getLabels() {
      // NOTE: This method can return null.
      if (v1Compatibility == null) {
        return null;
      }

      Map<String, String> labels = new HashMap<>();
      getLabelsFromPath("container_config.Labels", labels);
      getLabelsFromPath("config.Labels", labels);
      return labels;
    }

    private void getLabelsFromPath(String path, Map<String, String> finalLabels) {
      try {
        Map<String, String> labels = JsonUtils.jsonPath(v1Compatibility, path);
        if (isNotEmpty(labels)) {
          // NOTE: Removing labels where keys contain '.'. Storing and retrieving these keys is throwing error with
          // MongoDB and might also cause problems with expression evaluation as '.' is used as a separator there.
          labels.entrySet()
              .stream()
              .filter(entry -> !entry.getKey().contains("."))
              .forEach(entry -> finalLabels.putIfAbsent(entry.getKey(), entry.getValue()));
        }
      } catch (Exception ignored) {
        // Ignore error
      }
    }
  }

  public Map<String, String> fetchLabels() {
    // NOTE: This method should never return null.
    if (isEmpty(history)) {
      return new HashMap<>();
    }

    DockerImageManifestHistoryElement singleHistory = history.get(0);
    Map<String, String> labels = singleHistory.getLabels();
    return (labels == null) ? new HashMap<>() : labels;
  }
}
