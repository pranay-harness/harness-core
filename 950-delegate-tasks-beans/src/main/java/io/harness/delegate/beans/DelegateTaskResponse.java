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
  private boolean expired;

  public enum ResponseCode {
    OK,
    FAILED,
    RETRY_ON_OTHER_DELEGATE,
  }
}
