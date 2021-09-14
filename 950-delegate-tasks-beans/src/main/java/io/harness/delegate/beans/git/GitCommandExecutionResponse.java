/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.git;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.eraro.ErrorCode;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.GitBaseResult;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitCommandExecutionResponse implements DelegateTaskNotifyResponseData {
  private GitBaseResult gitCommandResult;
  private GitBaseRequest gitCommandRequest;
  private GitCommandStatus gitCommandStatus;
  private String errorMessage;
  private ErrorCode errorCode;
  private DelegateMetaInfo delegateMetaInfo;
  private ConnectorValidationResult connectorValidationResult;

  public enum GitCommandStatus { SUCCESS, FAILURE }
}
