/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobBuilder;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@AllArgsConstructor
@TypeAlias("verifyStepParameters")
@OwnedBy(HarnessTeam.CV)
@RecasterAlias("io.harness.cvng.cdng.beans.CVNGStepParameter")
public class CVNGStepParameter implements StepParameters {
  ParameterField<String> serviceIdentifier;
  ParameterField<String> envIdentifier;
  ParameterField<String> deploymentTag;
  ParameterField<String> sensitivity;
  VerificationJobBuilder verificationJobBuilder;

  public String getServiceIdentifier() {
    Preconditions.checkNotNull(serviceIdentifier.getValue());
    return serviceIdentifier.getValue();
  }
  public String getEnvIdentifier() {
    Preconditions.checkNotNull(envIdentifier.getValue());
    return envIdentifier.getValue();
  }
}
