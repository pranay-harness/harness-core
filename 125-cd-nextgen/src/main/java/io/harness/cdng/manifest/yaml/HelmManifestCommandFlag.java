package io.harness.cdng.manifest.yaml;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@EqualsAndHashCode(exclude = {"flag"})
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("helmManifestCommandFlag")
public class HelmManifestCommandFlag {
  @NotNull HelmCommandFlagType commandType;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> flag;
}
