/**
 *
 */

package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The type Execution credential.
 *
 * @author Rishi
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "executionType")
public abstract class ExecutionCredential {
  @JsonTypeId private ExecutionType executionType;

  ;

  /**
   * Instantiates a new Execution credential.
   *
   * @param executionType the execution type
   */
  protected ExecutionCredential(ExecutionType executionType) {
    this.executionType = executionType;
  }

  /**
   * Gets execution type.
   *
   * @return the execution type
   */
  public ExecutionType getExecutionType() {
    return executionType;
  }

  /**
   * Sets execution type.
   *
   * @param executionType the execution type
   */
  public void setExecutionType(ExecutionType executionType) {
    this.executionType = executionType;
  }

  /**
   * The enum Execution type.
   */
  public enum ExecutionType {
    /**
     * Ssh execution type.
     */
    SSH
  }
}
