package io.harness.beans.dependencies;

import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.data.validator.EntityIdentifier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 *  This stores specification for integration test services.
 */

@Data
@Builder
@JsonTypeName("service")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIServiceInfo implements DependencySpecType {
  @JsonIgnore public static final CIDependencyType type = CIDependencyType.SERVICE;

  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore @NotNull @EntityIdentifier private String identifier;
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore private String name;

  @JsonIgnore private Integer grpcPort;
  private Map<String, String> environment;
  private List<String> entrypoint;
  private List<String> args;

  private String image;
  private String connector;
  private ContainerResource resources;

  public CIDependencyType getType() {
    return type;
  }
}
