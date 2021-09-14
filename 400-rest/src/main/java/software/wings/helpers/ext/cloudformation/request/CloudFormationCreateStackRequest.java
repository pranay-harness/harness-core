/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.cloudformation.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;

import com.amazonaws.services.cloudformation.model.StackStatus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class CloudFormationCreateStackRequest extends CloudFormationCommandRequest {
  public static final String CLOUD_FORMATION_STACK_CREATE_URL = "Create URL";
  public static final String CLOUD_FORMATION_STACK_CREATE_BODY = "Create Body";
  public static final String CLOUD_FORMATION_STACK_CREATE_GIT = "Create GIT";
  private String createType;
  private String data;
  private String stackNameSuffix;
  private String customStackName;
  private Map<String, String> variables;
  private Map<String, EncryptedDataDetail> encryptedVariables;
  private GitFileConfig gitFileConfig;
  private List<EncryptedDataDetail> sourceRepoEncryptionDetails;
  private GitConfig gitConfig;
  private List<String> capabilities;
  private String tags;
  private List<StackStatus> stackStatusesToMarkAsSuccess;

  @Builder
  public CloudFormationCreateStackRequest(CloudFormationCommandType commandType, String accountId, String appId,
      String activityId, String commandName, AwsConfig awsConfig, int timeoutInMs, String createType, String data,
      String stackNameSuffix, String cloudFormationRoleArn, Map<String, String> variables, String region,
      String customStackName, GitFileConfig gitFileConfig, GitConfig gitConfig,
      List<StackStatus> stackStatusesToMarkAsSuccess, List<EncryptedDataDetail> encryptedDataDetails,
      Map<String, EncryptedDataDetail> encryptedVariables, List<String> capabilities, String tags) {
    super(
        commandType, accountId, appId, activityId, commandName, awsConfig, timeoutInMs, region, cloudFormationRoleArn);
    this.createType = createType;
    this.data = data;
    this.stackNameSuffix = stackNameSuffix;
    this.variables = variables;
    this.customStackName = customStackName;
    this.gitFileConfig = gitFileConfig;
    this.gitConfig = gitConfig;
    this.stackStatusesToMarkAsSuccess = stackStatusesToMarkAsSuccess;
    this.sourceRepoEncryptionDetails = encryptedDataDetails;
    this.encryptedVariables = encryptedVariables;
    this.capabilities = capabilities;
    this.tags = tags;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilities =
        new ArrayList<>(super.fetchRequiredExecutionCapabilities(maskingEvaluator));
    if (gitConfig != null && isNotEmpty(gitConfig.getDelegateSelectors())) {
      capabilities.add(SelectorCapability.builder().selectors(new HashSet<>(gitConfig.getDelegateSelectors())).build());
    }
    return capabilities;
  }
}
