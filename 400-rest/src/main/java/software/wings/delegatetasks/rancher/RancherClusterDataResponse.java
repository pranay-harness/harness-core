package software.wings.delegatetasks.rancher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

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