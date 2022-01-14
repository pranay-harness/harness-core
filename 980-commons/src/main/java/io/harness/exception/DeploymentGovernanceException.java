package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class DeploymentGovernanceException extends WingsException {
  public DeploymentGovernanceException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, ErrorCode.DEPLOYMENT_GOVERNANCE_ERROR, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }
}
