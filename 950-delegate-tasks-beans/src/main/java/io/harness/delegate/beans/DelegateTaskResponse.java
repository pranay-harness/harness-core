/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public class DelegateTaskResponse {
  private String accountId;
  private DelegateResponseData response;
  private ResponseCode responseCode;

  public enum ResponseCode {
    OK,
    FAILED,
    RETRY_ON_OTHER_DELEGATE,
  }
}
