package software.wings.helpers.ext.docker;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.JsonUtils;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

      try {
        Map<String, String> labels = JsonUtils.jsonPath(v1Compatibility, "container_config.Labels");
        if (isEmpty(labels)) {
          return labels;
        }
        // NOTE: Removing labels where keys contain '.'. Storing and retrieving these keys is throwing error with
        // MongoDB and might also cause problems with expression evaluation as '.' is used as a separator there.
        return labels.entrySet()
            .stream()
            .filter(entry -> !entry.getKey().contains("."))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      } catch (Exception e) {
        return null;
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
