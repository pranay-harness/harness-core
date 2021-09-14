/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.dashboard;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala on 06/30/19
 */
@Value
@Builder
public class DashboardAccessPermissions {
  private List<String> userGroups;
  private List<Action> allowedActions;
}
