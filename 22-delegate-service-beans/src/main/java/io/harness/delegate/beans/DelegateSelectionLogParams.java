package io.harness.delegate.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateSelectionLogParams {
  private String delegateId;
  private String delegateName;
  private String delegateHostName;
  private String delegateProfileName;
  private String conclusion;
  private String message;
  private long eventTimestamp;
}
