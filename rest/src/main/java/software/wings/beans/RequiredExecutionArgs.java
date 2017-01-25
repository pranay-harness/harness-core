/**
 *
 */

package software.wings.beans;

import java.util.HashSet;
import java.util.Set;

/**
 * The type Required execution args.
 *
 * @author Rishi
 */
public class RequiredExecutionArgs {
  private Set<EntityType> entityTypes = new HashSet<>();
  private ExecutionArgs defaultExecutionArgs = new ExecutionArgs();

  /**
   * Gets required execution types.
   *
   * @return the required execution types
   */
  public Set<EntityType> getEntityTypes() {
    return entityTypes;
  }

  /**
   * Sets required execution types.
   *
   * @param entityTypes the required execution types
   */
  public void setEntityTypes(Set<EntityType> entityTypes) {
    this.entityTypes = entityTypes;
  }

  public void addEntityType(EntityType entityType) {
    this.entityTypes.add(entityType);
  }

  /**
   * Gets default execution args.
   *
   * @return the default execution args
   */
  public ExecutionArgs getDefaultExecutionArgs() {
    return defaultExecutionArgs;
  }

  /**
   * Sets default execution args.
   *
   * @param defaultExecutionArgs the default execution args
   */
  public void setDefaultExecutionArgs(ExecutionArgs defaultExecutionArgs) {
    this.defaultExecutionArgs = defaultExecutionArgs;
  }
}
