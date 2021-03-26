package io.harness.steps.http;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.SwaggerConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.StepSpecTypeConstants;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
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
public class HttpStepParameters extends HttpBaseStepInfo implements StepParameters {
  String name;
  String identifier;
  String type = StepSpecTypeConstants.HTTP;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> description;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> skipCondition;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;
  Map<String, Object> outputVariables;
  Map<String, String> headers;
  ParameterField<List<TaskSelectorYaml>> delegateSelectors;

  @Builder(builderMethodName = "infoBuilder")
  public HttpStepParameters(ParameterField<String> url, ParameterField<String> method,
      ParameterField<String> requestBody, ParameterField<String> assertion, String name, String identifier,
      ParameterField<String> description, ParameterField<String> skipCondition, ParameterField<String> timeout,
      RollbackInfo rollbackInfo, Map<String, Object> outputVariables, Map<String, String> headers,
      ParameterField<List<TaskSelectorYaml>> delegateSelectors) {
    super(url, method, requestBody, assertion);
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.skipCondition = skipCondition;
    this.timeout = timeout;
    this.rollbackInfo = rollbackInfo;
    this.outputVariables = outputVariables;
    this.headers = headers;
    this.delegateSelectors = delegateSelectors;
    type = StepSpecTypeConstants.HTTP;
  }

  @Override
  public String toViewJson() {
    HttpStepParameters httpStepParameters = HttpStepParameters.infoBuilder()
                                                .assertion(getAssertion())
                                                .headers(headers)
                                                .method(getMethod())
                                                .outputVariables(outputVariables)
                                                .requestBody(getRequestBody())
                                                .timeout(timeout)
                                                .url(getUrl())
                                                .name(name)
                                                .identifier(identifier)
                                                .skipCondition(skipCondition)
                                                .description(description)
                                                .build();
    return RecastOrchestrationUtils.toDocumentJson(httpStepParameters);
  }
}
