/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.helpers.ext.jenkins;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
@OwnedBy(HarnessTeam.CDC)
public class SvnRevision {
  private String module;
  private int revision;

  /**
   * Gets module.
   *
   * @return the module
   */
  public String getModule() {
    return module;
  }

  /**
   * Sets module.
   *
   * @param module the module
   */
  public void setModule(String module) {
    this.module = module;
  }

  /**
   * Gets revision.
   *
   * @return the revision
   */
  public int getRevision() {
    return revision;
  }

  /**
   * Sets revision.
   *
   * @param revision the revision
   */
  public void setRevision(int revision) {
    this.revision = revision;
  }
}
