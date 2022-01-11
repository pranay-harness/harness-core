package software.wings.delegatetasks.rancher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RancherClusterDataResponse {
  String resourceType;
  List<ClusterData> data;

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ClusterData {
    private String id;
    private String name;
    private Map<String, String> labels;
  }
}