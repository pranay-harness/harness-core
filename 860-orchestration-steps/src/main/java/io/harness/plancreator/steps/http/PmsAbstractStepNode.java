/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.plancreator.steps.http;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Data;

@Data
@OwnedBy(PIPELINE)
@ApiModel(subTypes = {HttpStepInfo.class})
@JsonSubTypes({ @JsonSubTypes.Type(value = HttpStepInfo.class, name = StepSpecTypeConstants.HTTP) })
public abstract class PmsAbstractStepNode extends AbstractStepNode {
  List<FailureStrategyConfig> failureStrategies;
}
