/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "TaskDataKeys")
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
@OwnedBy(HarnessTeam.DEL)
public class TaskData {
  public static final long DEFAULT_SYNC_CALL_TIMEOUT = 60 * 1000L;
  public static final long DEFAULT_ASYNC_CALL_TIMEOUT = 10 * 60 * 1000L;

  private boolean parked;
  private boolean async;
  @NotNull private String taskType;
  private Object[] parameters;
  private long timeout;
  private int expressionFunctorToken;
  Map<String, String> expressions;
}
