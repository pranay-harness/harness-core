/**
 *
 */

package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * The type Execution credential.
 *
 * @author Rishi
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "executionType", include = As.EXISTING_PROPERTY)
@Data
@AllArgsConstructor
public abstract class ExecutionCredential {
  private ExecutionType executionType;

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
