package software.wings.beans;

import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.yaml.BaseEntityYaml;

/**
 * Marker base class for all deployment specifications
 * @author rktummala on 11/16/17
 */

public abstract class DeploymentSpecification extends Base {
  @Setter @Indexed private String accountId;

  @SchemaIgnore
  public String getAccountId() {
    return accountId;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public abstract static class Yaml extends BaseEntityYaml {
    public Yaml(String type, String harnessApiVersion) {
      super(type, harnessApiVersion);
    }
  }
}
