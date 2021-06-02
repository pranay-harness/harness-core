package io.harness.beans.dependencies;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.beans.common.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.beans.common.SwaggerConstants.STRING_LIST_CLASSPATH;
import static io.harness.beans.common.SwaggerConstants.STRING_MAP_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;
import io.harness.yaml.extended.ci.container.ContainerResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;

/**
 *  This stores specification for integration test services.
 */

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("ciServiceInfo")
@OwnedBy(CI)
public class CIServiceInfo implements DependencySpecType {
  @JsonIgnore public static final CIDependencyType type = CIDependencyType.SERVICE;

  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore @NotNull @EntityIdentifier private String identifier;
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore private String name;

  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore private Integer grpcPort;
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = STRING_MAP_CLASSPATH)
  private ParameterField<Map<String, String>> envVariables;
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = STRING_LIST_CLASSPATH)
  private ParameterField<List<String>> entrypoint;
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = STRING_LIST_CLASSPATH)
  private ParameterField<List<String>> args;

  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> image;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> connectorRef;
  private ContainerResource resources;

  @JsonIgnore
  public CIDependencyType getType() {
    return type;
  }
}
