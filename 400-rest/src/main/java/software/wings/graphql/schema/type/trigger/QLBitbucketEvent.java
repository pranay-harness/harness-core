/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.trigger;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
public enum QLBitbucketEvent {
  DIAGNOSTICS_PING,
  ANY,
  FORK,
  UPDATED,
  COMMIT_COMMENT_CREATED,
  BUILD_STATUS_CREATED,
  BUILD_STATUS_UPDATED,
  PUSH,
  REFS_CHANGED,
  ISSUE_ANY,
  ISSUE_CREATED,
  ISSUE_UPDATED,
  ISSUE_COMMENT_CREATED,
  PULL_REQUEST_ANY,
  PULL_REQUEST_CREATED,
  PULL_REQUEST_UPDATED,
  PULL_REQUEST_APPROVED,
  PULL_REQUEST_APPROVAL_REMOVED,
  PULL_REQUEST_MERGED,
  PULL_REQUEST_DECLINED,
  PULL_REQUEST_COMMENT_CREATED,
  PULL_REQUEST_COMMENT_UPDATED,
  PULL_REQUEST_COMMENT_DELETED
}
