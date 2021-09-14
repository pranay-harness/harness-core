/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopConsumer {
  @Id private String appId;
  private String appName;
  private String serviceId;
  private String serviceName;
  private int successfulActivityCount;
  private int failedActivityCount;
  private int totalCount;
}
