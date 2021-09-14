/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.expression.Expression.ALLOW_SECRETS;
import static io.harness.expression.Expression.DISALLOW_SECRETS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.capability.ProcessExecutionCapabilityHelper;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Builder
@Slf4j
@OwnedBy(CDP)
public class TerraformTaskNGParameters
    implements TaskParameters, ExecutionCapabilityDemander, NestedAnnotationResolver {
  @NonNull String accountId;
  String currentStateFileId;

  @NonNull TFTaskType taskType;
  @NonNull String entityId;
  String workspace;
  @NonNull GitFetchFilesConfig configFile;
  @Expression(ALLOW_SECRETS) List<TerraformVarFileInfo> varFileInfos;
  @Expression(ALLOW_SECRETS) String backendConfig;
  @Expression(DISALLOW_SECRETS) List<String> targets;
  @Expression(ALLOW_SECRETS) Map<String, String> environmentVariables;
  boolean saveTerraformStateJson;
  long timeoutInMillis;

  // For plan
  TerraformCommand terraformCommand;

  // To aid in logging
  TerraformCommandUnit terraformCommandUnit;

  // For Apply when inheriting from plan
  EncryptionConfig encryptionConfig;
  EncryptedRecordData encryptedTfPlan;
  String planName;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilities = ProcessExecutionCapabilityHelper.generateExecutionCapabilitiesForTerraform(
        configFile.getGitStoreDelegateConfig().getEncryptedDataDetails(), maskingEvaluator);
    log.info("Adding Required Execution Capabilities");
    if (configFile != null) {
      capabilities.add(GitConnectionNGCapability.builder()
                           .gitConfig((GitConfigDTO) configFile.getGitStoreDelegateConfig().getGitConfigDTO())
                           .encryptedDataDetails(configFile.getGitStoreDelegateConfig().getEncryptedDataDetails())
                           .sshKeySpecDTO(configFile.getGitStoreDelegateConfig().getSshKeySpecDTO())
                           .build());

      GitConfigDTO gitConfigDTO = (GitConfigDTO) configFile.getGitStoreDelegateConfig().getGitConfigDTO();
      if (isNotEmpty(gitConfigDTO.getDelegateSelectors())) {
        capabilities.add(SelectorCapability.builder().selectors(gitConfigDTO.getDelegateSelectors()).build());
      }
    }
    if (varFileInfos != null && isNotEmpty(varFileInfos)) {
      for (TerraformVarFileInfo varFileInfo : varFileInfos) {
        if (varFileInfo instanceof RemoteTerraformVarFileInfo) {
          GitFetchFilesConfig gitFetchFilesConfig = ((RemoteTerraformVarFileInfo) varFileInfo).getGitFetchFilesConfig();
          capabilities.add(
              GitConnectionNGCapability.builder()
                  .gitConfig((GitConfigDTO) gitFetchFilesConfig.getGitStoreDelegateConfig().getGitConfigDTO())
                  .encryptedDataDetails(gitFetchFilesConfig.getGitStoreDelegateConfig().getEncryptedDataDetails())
                  .sshKeySpecDTO(gitFetchFilesConfig.getGitStoreDelegateConfig().getSshKeySpecDTO())
                  .build());

          GitConfigDTO gitConfigDTO = (GitConfigDTO) gitFetchFilesConfig.getGitStoreDelegateConfig().getGitConfigDTO();
          if (isNotEmpty(gitConfigDTO.getDelegateSelectors())) {
            capabilities.add(SelectorCapability.builder().selectors(gitConfigDTO.getDelegateSelectors()).build());
          }
        }
      }
    }
    if (encryptionConfig != null) {
      capabilities.addAll(
          EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilityForSecretManager(encryptionConfig, null));
    }
    return capabilities;
  }
}
