package io.harness.pms.merger.fqn;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FQNNode {
  public enum NodeType { KEY, KEY_WITH_UUID, PARALLEL, UUID }

  private NodeType nodeType;
  private String key;
  private String uuidKey;
  private String uuidValue;
}
