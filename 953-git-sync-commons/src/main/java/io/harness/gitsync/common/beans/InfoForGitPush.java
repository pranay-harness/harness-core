package io.harness.gitsync.common.beans;

import io.harness.delegate.beans.connector.scm.ScmConnector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfoForGitPush {
  ScmConnector scmConnector;
  String filePath;
}
