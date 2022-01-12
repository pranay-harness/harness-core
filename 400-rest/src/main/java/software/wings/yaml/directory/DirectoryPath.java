/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.yaml.directory;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
public class DirectoryPath implements Cloneable {
  private static final String delimiter = "/";

  private String path;

  public DirectoryPath(String path) {
    this.path = path;
  }

  public DirectoryPath add(String pathPart) {
    if (isEmpty(path)) {
      this.path = pathPart;
    } else {
      this.path += delimiter + pathPart;
    }

    return this;
  }

  @Override
  public DirectoryPath clone() {
    return new DirectoryPath(this.path);
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
