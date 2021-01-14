package io.harness.beans.dependencies;

import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
  private ParameterField<Map<String, String>> envVariables;
  private ParameterField<List<String>> entrypoint;
  private ParameterField<List<String>> args;

  private ParameterField<String> image;
  private ParameterField<String> connectorRef;
  private ContainerResource resources;

  public CIDependencyType getType() {
    return type;
  }
}
