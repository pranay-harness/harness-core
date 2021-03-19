package software.wings.beans;

import io.harness.mongo.index.FdIndex;

import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Setter;

/**
 * Marker base class for all deployment specifications
 * @author rktummala on 11/16/17
 */

public abstract class DeploymentSpecification extends Base {
  @Setter @FdIndex private String accountId;

  @SchemaIgnore
  public String getAccountId() {
    return accountId;
  }
}
