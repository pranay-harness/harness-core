package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(DX)
public class InfoForGitPush {
  ScmConnector scmConnector;
  String folderPath;
  String filePath;
  boolean isDefault;
  String branch;
  boolean isNewBranch;
  String yamlGitConfigId;
  String defaultBranchName;
  String accountId;
  String projectIdentifier;
  String orgIdentifier;
  boolean executeOnDelegate;
  List<EncryptedDataDetail> encryptedDataDetailList;
}
