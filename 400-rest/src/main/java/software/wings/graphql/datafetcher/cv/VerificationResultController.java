/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
