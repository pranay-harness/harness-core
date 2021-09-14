/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notifications.conditions;

import java.util.List;

public interface ManualInterventionFilters {
  List<String> getAppIds();
  List<String> getEnvIds();
}
