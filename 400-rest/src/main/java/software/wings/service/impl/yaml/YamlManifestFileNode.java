package software.wings.service.impl.yaml;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YamlManifestFileNode {
  private String uuId;
  private String name;
  private boolean isDir;
  private String content;
  private Map<String, YamlManifestFileNode> childNodesMap;
}
