package io.harness.delegate.cf.retry;

import lombok.Builder;
import lombok.Data;

@Data
public class RetryPolicy {
  private int retry;
  private String userMessageOnFailure = "";
  private String finalErrorMessage = "";

  @Builder
  public RetryPolicy(int retry, String userMessageOnFailure, String finalErrorMessage) {
    this.retry = retry == 0 ? RetryAbleTaskExecutor.MIN_RETRY : retry;
    this.userMessageOnFailure = userMessageOnFailure;
    this.finalErrorMessage = finalErrorMessage;
  }
}
