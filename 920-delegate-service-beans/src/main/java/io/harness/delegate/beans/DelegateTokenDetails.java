package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.DEL)
public class DelegateTokenDetails {
  private String uuid;
  private String accountId;
  private String name;
  private EmbeddedUser createdBy;
  private long createdAt;
  private DelegateTokenStatus status;
  private String value;
  private String identifier;
}
