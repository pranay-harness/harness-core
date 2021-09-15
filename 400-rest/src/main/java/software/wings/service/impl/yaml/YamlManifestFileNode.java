/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
