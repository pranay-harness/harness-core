package software.wings.beans.container;

import com.github.reinert.jjschema.Attributes;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.yaml.BaseYaml;

@Data
@Builder
public class PortMapping {
  @Attributes(title = "Container port") private Integer containerPort;
  @Attributes(title = "Host port") private Integer hostPort;
  @Attributes(title = "Expose on Load Balancer") private boolean loadBalancerPort;

  @Data
  @EqualsAndHashCode(callSuper = true)
  @Builder
  public static final class Yaml extends BaseYaml {
    private Integer containerPort;
    private Integer hostPort;
    private boolean loadBalancerPort;
  }
}