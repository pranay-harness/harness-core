/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.beans.recommendation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResourceId {
  public static final ResourceId NOT_FOUND = ResourceId.builder().build();

  String accountId;
  String clusterId;
  String namespace;
  String name;
  String kind;
}
