/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.ci.vm;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.ci.CITaskExecutionResponse;
import io.harness.logging.CommandExecutionStatus;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VmTaskExecutionResponse implements CITaskExecutionResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private String errorMessage;
  private String ipAddress;
  private Map<String, String> outputVars;
  private CommandExecutionStatus commandExecutionStatus;
  @Builder.Default private static final CITaskExecutionResponse.Type type = Type.VM;

  @Override
  public CITaskExecutionResponse.Type getType() {
    return type;
  }
}
