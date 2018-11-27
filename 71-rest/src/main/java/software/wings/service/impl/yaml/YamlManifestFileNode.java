package software.wings.service.impl.yaml;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class YamlManifestFileNode {
  private String uuId;
  private String name;
  private boolean isDir;
  private String content;
  private Map<String, YamlManifestFileNode> childNodesMap;
}
