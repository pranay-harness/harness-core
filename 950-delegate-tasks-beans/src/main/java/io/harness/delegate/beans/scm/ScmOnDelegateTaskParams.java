package io.harness.delegate.beans.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@OwnedBy(HarnessTeam.DX)
public class ScmOnDelegateTaskParams {
  ScmConnector scmConnector;
  GitFileDetails gitFileDetails;
}
