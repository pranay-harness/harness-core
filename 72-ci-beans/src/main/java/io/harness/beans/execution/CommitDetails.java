package io.harness.beans.execution;

import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
public class CommitDetails {
  @NotEmpty private String commitId;
  private String link;
  private String message;
  private String ownerName;
  private String ownerId;
  private String ownerEmail;
}
