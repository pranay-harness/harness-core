/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.beans;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@Getter
@OwnedBy(CE)
public class JobConstants {
  protected String accountId;
  protected long jobStartTime;
  protected long jobEndTime;
}
