/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
@OwnedBy(HarnessTeam.CV)
public enum ElkValidationType {
  PASSWORD("Password"),
  TOKEN("API Token");

  private String name;

  ElkValidationType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
