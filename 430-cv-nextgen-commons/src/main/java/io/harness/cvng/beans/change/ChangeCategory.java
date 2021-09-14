/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.change;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ChangeCategory {
  @JsonProperty("Deployment") DEPLOYMENT,
  @JsonProperty("Infrastructure") INFRASTRUCTURE,
  @JsonProperty("Alert") ALERTS
}
