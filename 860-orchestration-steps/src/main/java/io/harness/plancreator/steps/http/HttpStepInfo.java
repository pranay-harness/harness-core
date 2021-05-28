package io.harness.plancreator.steps.http;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.http.HttpHeaderConfig;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.http.HttpBaseStepInfo;
import io.harness.steps.http.HttpStep;
import io.harness.steps.http.HttpStepParameters;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.utils.NGVariablesUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.HTTP)
@SimpleVisitorHelper(helperClass = HttpStepInfoVisitorHelper.class)
@TypeAlias("httpStepInfo")
@OwnedBy(PIPELINE)
public class HttpStepInfo extends HttpBaseStepInfo implements PMSStepInfo, Visitable {
  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  List<NGVariable> outputVariables;
  List<HttpHeaderConfig> headers;
  ParameterField<List<TaskSelectorYaml>> delegateSelectors;

  @Builder(builderMethodName = "infoBuilder")
  public HttpStepInfo(ParameterField<String> url, ParameterField<String> method, ParameterField<String> requestBody,
      ParameterField<String> assertion, String metadata, List<NGVariable> outputVariables,
      List<HttpHeaderConfig> headers, ParameterField<List<TaskSelectorYaml>> delegateSelectors) {
    super(url, method, requestBody, assertion);
    this.metadata = metadata;
    this.outputVariables = outputVariables;
    this.headers = headers;
    this.delegateSelectors = delegateSelectors;
  }

  @Override
  @JsonIgnore
  public StepType getStepType() {
    return HttpStep.STEP_TYPE;
  }

  @Override
  @JsonIgnore
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    return VisitableChildren.builder().build();
  }

  @Override
  public SpecParameters getSpecParameters() {
    return HttpStepParameters.infoBuilder()
        .assertion(getAssertion())
        .headers(headers.stream().collect(Collectors.toMap(HttpHeaderConfig::getKey, HttpHeaderConfig::getValue)))
        .method(getMethod())
        .outputVariables(NGVariablesUtils.getMapOfVariables(outputVariables, 0L))
        .requestBody(getRequestBody())
        .delegateSelectors(ParameterField.createValueField(
            CollectionUtils.emptyIfNull(delegateSelectors != null ? delegateSelectors.getValue() : null)))
        .url(getUrl())
        .build();
  }
}
