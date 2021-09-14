/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.execution;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Repository {
  private String name;
  private String namespace;
  private String link;
  private String branch;
  private boolean isPrivate;
  private String httpURL;
  private String sshURL;
  private String slug; // Repository name along with namespace
}
