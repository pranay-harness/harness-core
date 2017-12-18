package software.wings.beans;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYamlWithType;

/**
 * Marker base class for all deployment specifications
 * @author rktummala on 11/16/17
 */
public abstract class DeploymentSpecification extends Base {
  @Data
  @EqualsAndHashCode(callSuper = false)
  @NoArgsConstructor
  public static abstract class Yaml extends BaseYamlWithType {
    public Yaml(String type) {
      super(type);
    }
  }
}
