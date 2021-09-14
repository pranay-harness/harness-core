/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.instance;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
final class ArtifactInfo {
  private String id;
  private String name;
  private String buildNo;
  private String streamId;
  private String streamName;
  private long deployedAt;
  private String sourceName;
  private String lastWorkflowExecutionId;
}
