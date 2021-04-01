package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(DX)
public class InfoForGitPush {
  ScmConnector scmConnector;
  String filePath;
}
