package software.wings.beans.container;

import com.github.reinert.jjschema.Attributes;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYaml;

@Data
@Builder
public class PortMapping {
  @Attributes(title = "Container port") private Integer containerPort;
  @Attributes(title = "Host port") private Integer hostPort;
  @Attributes(title = "Expose on Load Balancer") private boolean loadBalancerPort;

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseYaml {
    private Integer containerPort;
    private Integer hostPort;
    private boolean loadBalancerPort;

    @Builder
    public Yaml(Integer containerPort, Integer hostPort, boolean loadBalancerPort) {
      this.containerPort = containerPort;
      this.hostPort = hostPort;
      this.loadBalancerPort = loadBalancerPort;
    }
  }
}