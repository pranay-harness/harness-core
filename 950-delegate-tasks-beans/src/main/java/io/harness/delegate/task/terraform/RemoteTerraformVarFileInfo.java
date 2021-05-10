package io.harness.delegate.task.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.git.GitFetchFilesConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class RemoteTerraformVarFileInfo implements TerraformVarFileInfo {
  GitFetchFilesConfig gitFetchFilesConfig;
}
