/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.helpers.ext.jenkins;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
@OwnedBy(HarnessTeam.CDC)
@TargetModule(_960_API_SERVICES)
public class SvnChangeSet {
  private List<SvnRevision> revisions;

  /**
   * Gets revisions.
   *
   * @return the revisions
   */
  public List<SvnRevision> getRevisions() {
    return revisions;
  }

  /**
   * Sets revisions.
   *
   * @param revisions the revisions
   */
  public void setRevisions(List<SvnRevision> revisions) {
    this.revisions = revisions;
  }
}
