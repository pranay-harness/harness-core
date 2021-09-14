/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.http;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.SkipAutoEvaluation;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("httpStepParameters")
@RecasterAlias("io.harness.steps.http.HttpStepParameters")
public class HttpStepParameters extends HttpBaseStepInfo implements SpecParameters {
  @SkipAutoEvaluation ParameterField<Map<String, Object>> outputVariables;
  Map<String, String> headers;
  ParameterField<List<TaskSelectorYaml>> delegateSelectors;

  @Builder(builderMethodName = "infoBuilder")
  public HttpStepParameters(ParameterField<String> url, ParameterField<String> method,
      ParameterField<String> requestBody, ParameterField<String> assertion, Map<String, Object> outputVariables,
      Map<String, String> headers, ParameterField<List<TaskSelectorYaml>> delegateSelectors) {
    super(url, method, requestBody, assertion);
    this.outputVariables = ParameterField.createValueField(outputVariables);
    this.headers = headers;
    this.delegateSelectors = delegateSelectors;
  }
}
