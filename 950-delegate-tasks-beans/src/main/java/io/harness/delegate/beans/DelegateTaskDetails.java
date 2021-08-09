package io.harness.delegate.beans;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DelegateTaskDetailsKeys")
public class DelegateTaskDetails {
  private String delegateTaskId;
  private String taskDescription;
  private String selectedDelegateId;
  private String selectedDelegateName;
  private String selectedDelegateHostName;
  private Map<String, String> setupAbstractions;
  /**
   * @deprecated Use taskDescription instead.
   */
  @Deprecated private String taskType;
}
