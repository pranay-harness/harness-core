/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.api.AwsCodeDeployRequestElement.AWS_CODE_DEPLOY_REQUEST_PARAM;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.api.AwsCodeDeployRequestElement;
import software.wings.api.CommandStateExecutionData.Builder;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CodeDeployParams;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;

/**
 * Created by rishi on 6/26/17.
 */
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class AwsCodeDeployRollback extends AwsCodeDeployState {
  public AwsCodeDeployRollback(String name) {
    super(name, StateType.AWS_CODEDEPLOY_ROLLBACK.name());
  }

  @Override
  protected CodeDeployParams prepareCodeDeployParams(ExecutionContext context,
      CodeDeployInfrastructureMapping infrastructureMapping, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, Builder executionDataBuilder) {
    AwsCodeDeployRequestElement codeDeployRequestElement =
        context.getContextElement(ContextElementType.PARAM, AWS_CODE_DEPLOY_REQUEST_PARAM);
    executionDataBuilder.withCodeDeployParams(codeDeployRequestElement.getOldCodeDeployParams());
    return codeDeployRequestElement.getOldCodeDeployParams();
  }

  @Override
  @SchemaIgnore
  public String getBucket() {
    return super.getBucket();
  }

  @Override
  @SchemaIgnore
  public String getKey() {
    return super.getKey();
  }

  @Override
  @SchemaIgnore
  public String getBundleType() {
    return super.getBundleType();
  }

  @Override
  @SchemaIgnore
  public boolean isIgnoreApplicationStopFailures() {
    return super.isIgnoreApplicationStopFailures();
  }

  @Override
  @SchemaIgnore
  public String getFileExistsBehavior() {
    return super.getFileExistsBehavior();
  }

  @Override
  @SchemaIgnore
  public boolean isEnableAutoRollback() {
    return super.isEnableAutoRollback();
  }

  @Override
  @SchemaIgnore
  public List<String> getAutoRollbackConfigurations() {
    return super.getAutoRollbackConfigurations();
  }
}
