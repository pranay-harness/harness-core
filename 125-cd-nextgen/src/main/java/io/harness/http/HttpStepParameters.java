package io.harness.http;

import static io.harness.annotations.dev.HarnessTeam.NG;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.SwaggerConstants;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;

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

@OwnedBy(NG)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("httpStepParameters")
public class HttpStepParameters extends HttpBaseStepInfo implements StepParameters {
  String name;
  String identifier;
  String type = StepSpecTypeConstants.HTTP;
  ParameterField<String> description;
  ParameterField<String> skipCondition;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;
  Map<String, Object> outputVariables;

  @Builder(builderMethodName = "infoBuilder")
  public HttpStepParameters(ParameterField<String> url, ParameterField<String> method, List<HttpHeaderConfig> headers,
      ParameterField<String> requestBody, ParameterField<String> assertion, String name, String identifier,
      ParameterField<String> description, ParameterField<String> skipCondition, ParameterField<String> timeout,
      RollbackInfo rollbackInfo, Map<String, Object> outputVariables) {
    super(url, method, headers, requestBody, assertion);
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.skipCondition = skipCondition;
    this.timeout = timeout;
    this.rollbackInfo = rollbackInfo;
    this.outputVariables = outputVariables;
    type = StepSpecTypeConstants.HTTP;
  }

  @Override
  public String toViewJson() {
    HttpStepParameters httpStepParameters = HttpStepParameters.infoBuilder()
                                                .assertion(getAssertion())
                                                .headers(getHeaders())
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
