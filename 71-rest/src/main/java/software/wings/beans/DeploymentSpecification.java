package software.wings.beans;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.yaml.BaseEntityYaml;

/**
 * Marker base class for all deployment specifications
 * @author rktummala on 11/16/17
 */
@Data
public abstract class DeploymentSpecification extends Base {
  @Indexed private String accountId;
  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public abstract static class Yaml extends BaseEntityYaml {
    public Yaml(String type, String harnessApiVersion) {
      super(type, harnessApiVersion);
    }
  }
}
