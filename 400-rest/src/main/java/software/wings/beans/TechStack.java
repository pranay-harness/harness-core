/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala on 05/28/19
 */
@Value
@Builder
public class TechStack {
  private String category;
  private String technology;
}
