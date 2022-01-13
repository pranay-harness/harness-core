/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
