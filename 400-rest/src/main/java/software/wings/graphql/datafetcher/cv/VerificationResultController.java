/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.cv;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.cv.WorkflowVerificationResult;

import software.wings.graphql.schema.type.QLVerificationResult.QLVerificationResultBuilder;

import lombok.experimental.UtilityClass;

@UtilityClass
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class VerificationResultController {
  public static QLVerificationResultBuilder populateQLApplication(WorkflowVerificationResult verificationResult,
      QLVerificationResultBuilder builder, String appName, String serviceName, String environmentName) {
    return builder.appName(appName)
        .serviceName(serviceName)
        .envName(environmentName)
        .type(verificationResult.getStateType())
        .status(verificationResult.getExecutionStatus().name())
        .analyzed(verificationResult.isAnalyzed())
        .rollback(verificationResult.isRollback())
        .message(verificationResult.getMessage())
        .startedAt(verificationResult.getCreatedAt());
  }
}
