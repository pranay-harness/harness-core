/**
 *
 */
package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;

import javax.validation.constraints.NotNull;

/**
 * The Class Orchestration.
 *
 * @author Rishi
 */
@Entity(value = "orchestrations", noClassnameStored = true)
public class Orchestration extends Workflow {
  @Indexed @Reference(idOnly = true) @NotNull private Environment environment;

  public Environment getEnvironment() {
    return environment;
  }

  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }
}
