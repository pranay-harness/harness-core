package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.NGAccess;

@OwnedBy(DX)
public interface GitSyncableEntity extends NGAccess {
  String getUuid();

  String getObjectIdOfYaml();

  void setObjectIdOfYaml(String objectIdOfYaml);

  Boolean getIsFromDefaultBranch();

  void setIsFromDefaultBranch(Boolean isFromDefaultBranch);

  void setBranch(String branch);

  String getBranch();

  String getYamlGitConfigRef();

  void setYamlGitConfigRef(String yamlGitConfigRef);

  String getRootFolder();

  void setRootFolder(String rootFolder);

  String getFilePath();

  void setFilePath(String filePath);

  boolean isEntityInvalid();

  void setEntityInvalid(boolean isEntityInvalid);

  String getInvalidYamlString();
}
