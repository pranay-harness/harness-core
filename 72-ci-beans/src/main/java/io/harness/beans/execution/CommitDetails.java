package io.harness.beans.execution;

import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
public class CommitDetails {
  @NotEmpty private String commitId;
  private String link;
  private String description;
  private String userName;
  private String ownerId;
  private String ownerEmail;
}
