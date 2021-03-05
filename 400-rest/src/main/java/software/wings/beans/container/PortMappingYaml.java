package software.wings.beans.container;

import io.harness.yaml.BaseYaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class PortMappingYaml extends BaseYaml {
  private Integer containerPort;
  private Integer hostPort;

  @Builder
  public PortMappingYaml(Integer containerPort, Integer hostPort) {
    this.containerPort = containerPort;
    this.hostPort = hostPort;
  }
}
