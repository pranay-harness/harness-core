package io.harness.pms.cdng.service.beans;

import io.harness.beans.ParameterField;
import io.harness.common.SwaggerConstants;

import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("serviceUseFromStagePms")
public class ServiceUseFromStagePms implements Serializable {
  String uuid;
  // Stage identifier of the stage to select from.
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @NotNull ParameterField<String> stage;
}
