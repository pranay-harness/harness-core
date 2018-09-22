package software.wings.beans.container;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYaml;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class PortMapping {
  @Attributes(title = "Container port") private Integer containerPort;
  @Attributes(title = "Host port") private Integer hostPort;

  @JsonIgnoreProperties(ignoreUnknown = true)
  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseYaml {
    private Integer containerPort;
    private Integer hostPort;

    @Builder
    public Yaml(Integer containerPort, Integer hostPort) {
      this.containerPort = containerPort;
      this.hostPort = hostPort;
    }
  }
}
