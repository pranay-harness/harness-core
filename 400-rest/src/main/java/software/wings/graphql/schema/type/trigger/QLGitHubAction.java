/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.type.trigger;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.EnumSet;
import java.util.Set;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
public enum QLGitHubAction {
  CLOSED,
  EDITED,
  OPENED,
  REOPENED,
  ASSIGNED,
  UNASSIGNED,
  LABELED,
  UNLABELED,
  SYNCHRONIZED,
  REVIEW_REQUESTED,
  REVIEW_REQUESTED_REMOVED,
  PACKAGE_PUBLISHED,
  CREATED,
  PUBLISHED,
  RELEASED,
  UNPUBLISHED,
  DELETED,
  PRE_RELEASED;

  public static final Set<QLGitHubAction> pullRequestActions = EnumSet.of(ASSIGNED, CLOSED, EDITED, LABELED, OPENED,
      REVIEW_REQUESTED, REVIEW_REQUESTED_REMOVED, REOPENED, SYNCHRONIZED, UNASSIGNED, UNLABELED);
  public static final Set<QLGitHubAction> releaseActions =
      EnumSet.of(CREATED, DELETED, EDITED, PRE_RELEASED, PUBLISHED, RELEASED, UNPUBLISHED);
  public static final Set<QLGitHubAction> packageActions = EnumSet.of(PACKAGE_PUBLISHED);
}
