/**
 *
 */
package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Rishi
 *
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "executionType")
@JsonSubTypes({ @Type(value = SSHExecutionCredential.class, name = "SSH") })

public abstract class ExecutionCredential {
  public enum ExecutionType { SSH }
  ;

  private ExecutionType executionType;

  protected ExecutionCredential(ExecutionType executionType) {
    this.executionType = executionType;
  }

  public ExecutionType getExecutionType() {
    return executionType;
  }

  public void setExecutionType(ExecutionType executionType) {
    this.executionType = executionType;
  }
}
