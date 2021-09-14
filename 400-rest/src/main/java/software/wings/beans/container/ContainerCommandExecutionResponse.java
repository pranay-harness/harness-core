/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.container;

import io.harness.delegate.beans.DelegateResponseData;

import software.wings.beans.yaml.GitCommandRequest;
import software.wings.beans.yaml.GitCommandResult;

import lombok.Builder;
import lombok.Data;

/**
 * Created by brett on 11/29/17.
 */
@Data
@Builder
public class ContainerCommandExecutionResponse implements DelegateResponseData {
  private GitCommandResult gitCommandResult;
  private GitCommandRequest gitCommandRequest;
  private GitCommandStatus gitCommandStatus;
  private String errorMessage;

  public enum GitCommandStatus { SUCCESS, FAILURE }
}
