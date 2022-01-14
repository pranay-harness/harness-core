package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@OwnedBy(DX)
public class CommitDetails {
  @NotEmpty private String commitId;
  private String link;
  private String message;
  private String ownerName;
  private String ownerId;
  private String ownerEmail;
  private long timeStamp;
}
