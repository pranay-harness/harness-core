/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Created by rsingh on 9/5/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicErrors {
  private long errors_per_minute;
  private long error_count;
}
