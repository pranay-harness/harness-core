/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.terragrunt.v2;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_APPLY_COMMAND_FORMAT_WITH_PLAN_INPUT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_DESTROY_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_INFO_COMMAND;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_INIT_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_OUTPUT_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_PLAN_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_PLAN_DESTROY_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_REFRESH_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_RUN_ALL_APPLY_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_RUN_ALL_DESTROY_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_RUN_ALL_INIT_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_RUN_ALL_OUTPUT_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_RUN_ALL_PLAN_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_RUN_ALL_PLAN_DESTROY_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_RUN_ALL_REFRESH_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_RUN_ALL_SHOW_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_SHOW_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_VERSION_COMMAND;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_WORKSPACE_LIST_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_WORKSPACE_NEW_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_WORKSPACE_RUN_ALL_NEW_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_WORKSPACE_RUN_ALL_SELECT_COMMAND_FORMAT;
import static io.harness.provision.TerragruntConstants.TERRAGRUNT_WORKSPACE_SELECT_COMMAND_FORMAT;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.terragrunt.v2.request.TerragruntRunType;

import java.io.File;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDP)
@UtilityClass
public class TerragruntCommandUtils {
  public String init(String backendConfigFilePath, TerragruntRunType runType) {
    File backendConfigFile = null;
    if (StringUtils.isNotBlank(backendConfigFilePath)) {
      backendConfigFile = new File(backendConfigFilePath);
    }

    String initCommand =
        runType == TerragruntRunType.RUN_ALL ? TERRAGRUNT_RUN_ALL_INIT_COMMAND_FORMAT : TERRAGRUNT_INIT_COMMAND_FORMAT;
    return format(initCommand,
        backendConfigFile != null && backendConfigFile.exists() ? format(" -backend-config=%s", backendConfigFilePath)
                                                                : "");
  }

  public String refresh(String targetArgs, String varParams) {
    return format(TERRAGRUNT_REFRESH_COMMAND_FORMAT, targetArgs.trim(), varParams.trim());
  }

  public String runAllRefresh(String targetArgs, String varParams) {
    return format(TERRAGRUNT_RUN_ALL_REFRESH_COMMAND_FORMAT, targetArgs.trim(), varParams.trim());
  }

  public String plan(String targetArgs, String varParams, boolean destroy) {
    return format(destroy ? TERRAGRUNT_PLAN_DESTROY_COMMAND_FORMAT : TERRAGRUNT_PLAN_COMMAND_FORMAT, targetArgs.trim(),
        varParams.trim());
  }

  public String runAllPlan(String targetArgs, String varParams, boolean destroy) {
    return format(destroy ? TERRAGRUNT_RUN_ALL_PLAN_DESTROY_COMMAND_FORMAT : TERRAGRUNT_RUN_ALL_PLAN_COMMAND_FORMAT,
        targetArgs.trim(), varParams.trim());
  }

  public String apply(String terraformPlanName) {
    return format(TERRAGRUNT_APPLY_COMMAND_FORMAT_WITH_PLAN_INPUT, terraformPlanName);
  }

  public String runAllApply(String targetArgs, String varParams) {
    return format(TERRAGRUNT_RUN_ALL_APPLY_COMMAND_FORMAT, targetArgs, varParams);
  }

  public String destroy(String autoApproveArg, String targetArgs, String varParams) {
    return format(TERRAGRUNT_DESTROY_COMMAND_FORMAT, autoApproveArg, targetArgs, varParams);
  }

  public String runAllDestroy(String autoApproveArg, String targetArgs, String varParams) {
    return format(TERRAGRUNT_RUN_ALL_DESTROY_COMMAND_FORMAT, autoApproveArg, targetArgs, varParams);
  }

  public String output(String outputFilePath) {
    return format(TERRAGRUNT_OUTPUT_COMMAND_FORMAT, outputFilePath);
  }

  public String runAllOutput(String outputFilePath) {
    return format(TERRAGRUNT_RUN_ALL_OUTPUT_COMMAND_FORMAT, outputFilePath);
  }

  public String show(boolean json, String planName) {
    return format(TERRAGRUNT_SHOW_COMMAND_FORMAT, json ? "-json " + planName : planName);
  }

  public String runAllShow(boolean json, String planName) {
    return format(TERRAGRUNT_RUN_ALL_SHOW_COMMAND_FORMAT, json ? "-json " + planName : planName);
  }

  public String info() {
    return TERRAGRUNT_INFO_COMMAND;
  }

  public String version() {
    return TERRAGRUNT_VERSION_COMMAND;
  }

  public String workspaceList() {
    return TERRAGRUNT_WORKSPACE_LIST_COMMAND_FORMAT;
  }

  public String workspaceNew(String workspace, TerragruntRunType type) {
    String workspaceNewCmd = type == TerragruntRunType.RUN_ALL ? TERRAGRUNT_WORKSPACE_RUN_ALL_NEW_COMMAND_FORMAT
                                                               : TERRAGRUNT_WORKSPACE_NEW_COMMAND_FORMAT;
    return format(workspaceNewCmd, workspace);
  }

  public String workspaceSelect(String workspace, TerragruntRunType type) {
    String workspaceSelectCmd = type == TerragruntRunType.RUN_ALL ? TERRAGRUNT_WORKSPACE_RUN_ALL_SELECT_COMMAND_FORMAT
                                                                  : TERRAGRUNT_WORKSPACE_SELECT_COMMAND_FORMAT;
    return format(workspaceSelectCmd, workspace);
  }
}
