package software.wings.beans;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseEntityYaml;

/**
 * Marker base class for all deployment specifications
 * @author rktummala on 11/16/17
 */
public abstract class DeploymentSpecification extends Base {
  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public abstract static class Yaml extends BaseEntityYaml {
    public Yaml(String type, String harnessApiVersion) {
      super(type, harnessApiVersion);
    }
  }
}
