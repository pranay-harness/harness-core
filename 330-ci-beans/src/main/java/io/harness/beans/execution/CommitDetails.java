/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
  private long timeStamp;
}
