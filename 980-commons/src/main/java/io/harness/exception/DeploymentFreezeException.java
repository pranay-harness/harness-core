/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeploymentFreezeException extends WingsException {
  private String accountId;
  private boolean masterFreeze;
  private List<String> deploymentFreezeIds;
  private String deploymentFreezeNames;

  private static final String MESSAGE_KEY = "message";
  public DeploymentFreezeException(ErrorCode code, Level level, EnumSet<ReportTarget> reportTargets, String accountId,
      List<String> deploymentFreezeIds, String deploymentFreezeNames, boolean masterFreeze) {
    super(getErrorMessage(deploymentFreezeNames, deploymentFreezeIds.size(), masterFreeze), null, code, level,
        reportTargets, null);
    param(MESSAGE_KEY, getErrorMessage(deploymentFreezeNames, deploymentFreezeIds.size(), masterFreeze));
    this.masterFreeze = masterFreeze;
    this.deploymentFreezeIds = deploymentFreezeIds;
    this.accountId = accountId;
    this.deploymentFreezeNames = deploymentFreezeNames;
  }

  @Override
  public String getMessage() {
    return getErrorMessage(deploymentFreezeNames, deploymentFreezeIds.size(), masterFreeze);
  }

  private static String getErrorMessage(String names, double freezeWindowCount, boolean masterFreeze) {
    if (masterFreeze) {
      return "Master Deployment Freeze is active. No deployments are allowed.";
    }
    return String.format(
        "Deployment Freeze Window%s %s %s active for the environment. No deployments are allowed to proceed.",
        freezeWindowCount > 1 ? "s" : "", names, freezeWindowCount > 1 ? "are" : "is");
  }
}
