/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PipelineElement {
  private String name;
  private String displayName;
  private String description;
  private Long startTs;
}
