package io.harness.http;

import static io.harness.annotations.dev.HarnessTeam.NG;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.SwaggerConstants;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariable;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
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
  ParameterField<String> description;
  ParameterField<String> skipCondition;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;

  @Builder(builderMethodName = "infoBuilder")
  public HttpStepParameters(String name, String identifier, ParameterField<String> description,
      ParameterField<String> skipCondition, ParameterField<String> url, ParameterField<String> method,
      List<HttpHeaderConfig> headers, ParameterField<String> requestBody, ParameterField<String> assertion,
      List<NGVariable> outputVariables, ParameterField<String> timeout, RollbackInfo rollbackInfo) {
    super(url, method, headers, requestBody, assertion, outputVariables);
    this.timeout = timeout;
    this.rollbackInfo = rollbackInfo;
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.skipCondition = skipCondition;
  }

  @Override
  public String toViewJson() {
    HttpStepParameters httpStepParameters = HttpStepParameters.infoBuilder()
                                                .assertion(getAssertion())
                                                .headers(getHeaders())
                                                .method(getMethod())
                                                .outputVariables(getOutputVariables())
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
