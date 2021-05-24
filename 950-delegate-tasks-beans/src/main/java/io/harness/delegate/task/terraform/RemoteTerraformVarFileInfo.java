package io.harness.delegate.task.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class RemoteTerraformVarFileInfo implements TerraformVarFileInfo, NestedAnnotationResolver {
  GitFetchFilesConfig gitFetchFilesConfig;
}
