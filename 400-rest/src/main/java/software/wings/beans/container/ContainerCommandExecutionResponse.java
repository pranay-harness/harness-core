/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
