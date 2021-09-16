package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.SkipAutoEvaluation;

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
@OwnedBy(CDC)
public class HelmManifestCommandFlag {
  @NotNull HelmCommandFlagType commandType;
  @SkipAutoEvaluation @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> flag;
}
