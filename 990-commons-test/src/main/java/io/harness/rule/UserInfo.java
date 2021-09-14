/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.rule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserInfo {
  private String email;
  private String slack;
  private String jira;
  private String team;
}
