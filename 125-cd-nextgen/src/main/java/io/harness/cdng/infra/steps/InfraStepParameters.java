/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.infra.steps;

import io.harness.annotation.RecasterAlias;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("infraStepParameters")
@RecasterAlias("io.harness.cdng.infra.steps.InfraStepParameters")
public class InfraStepParameters implements StepParameters {
  PipelineInfrastructure pipelineInfrastructure;
  Infrastructure infrastructure;
}
