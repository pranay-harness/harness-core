/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ci.stdvars;

import io.harness.validation.Update;

import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
public class GitVariables {
  private String tag;
  private String revision;
  private String targetRepo;
  private String sourceRepo;
  private String targetBranch;
  private String sourceBranch;
  private boolean isPullRequest;
  private boolean pullRequestID;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}
