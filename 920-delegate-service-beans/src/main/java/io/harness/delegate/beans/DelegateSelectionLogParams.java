package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.DEL)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateSelectionLogParams {
  private String delegateId;
  private String delegateType;
  private String delegateName;
  private String delegateHostName;
  private String delegateProfileName;
  private String conclusion;
  private String message;
  private long eventTimestamp;
  private ProfileScopingRulesDetails profileScopingRulesDetails;
}
