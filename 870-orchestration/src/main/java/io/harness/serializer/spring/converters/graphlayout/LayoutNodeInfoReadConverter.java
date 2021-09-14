/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.spring.converters.graphlayout;

import io.harness.pms.contracts.plan.GraphLayoutInfo;
import io.harness.serializer.spring.ProtoReadConverter;

public class LayoutNodeInfoReadConverter extends ProtoReadConverter<GraphLayoutInfo> {
  public LayoutNodeInfoReadConverter() {
    super(GraphLayoutInfo.class);
  }
}
