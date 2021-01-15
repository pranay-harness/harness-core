package io.harness.beans.dependencies;

import static io.harness.common.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.common.SwaggerConstants.STRING_LIST_CLASSPATH;
import static io.harness.common.SwaggerConstants.STRING_MAP_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.list;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.map;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

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
public class CIServiceInfo implements DependencySpecType {
  @JsonIgnore public static final CIDependencyType type = CIDependencyType.SERVICE;

  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore @NotNull @EntityIdentifier private String identifier;
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore private String name;

  @JsonIgnore private Integer grpcPort;
  @YamlSchemaTypes(value = {map, string}, defaultType = map)
  @ApiModelProperty(dataType = STRING_MAP_CLASSPATH)
  private ParameterField<Map<String, String>> envVariables;
  @YamlSchemaTypes(value = {list, string}, defaultType = list)
  @ApiModelProperty(dataType = STRING_LIST_CLASSPATH)
  private ParameterField<List<String>> entrypoint;
  @YamlSchemaTypes(value = {list, string}, defaultType = list)
  @ApiModelProperty(dataType = STRING_LIST_CLASSPATH)
  private ParameterField<List<String>> args;

  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> image;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> connectorRef;
  private ContainerResource resources;

  public CIDependencyType getType() {
    return type;
  }
}
