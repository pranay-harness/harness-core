/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.yaml.directory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
public class FileNode extends DirectoryNode {
  public FileNode() {
    this.setType(NodeType.FILE);
  }

  public FileNode(String accountId, String name, Class theClass) {
    super(accountId, name, theClass);
    this.setType(NodeType.FILE);
  }

  public FileNode(String accountId, String name, Class theClass, DirectoryPath directoryPath) {
    super(accountId, name, theClass, directoryPath, NodeType.FILE);
  }
}
