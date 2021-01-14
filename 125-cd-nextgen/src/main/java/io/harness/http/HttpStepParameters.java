package io.harness.http;

import static io.harness.annotations.dev.HarnessTeam.NG;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.SwaggerConstants;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariable;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(NG)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("httpStepParameters")
public class HttpStepParameters implements StepParameters {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> url;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> method;
  List<HttpHeaderConfig> headers;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> requestBody;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> assertion;
  List<NGVariable> outputVariables;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
}
