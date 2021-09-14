/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.pms.resume.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;

import com.google.protobuf.ByteString;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface NodeResumeEventPublisher {
  void publishEvent(NodeExecution nodeExecution, Map<String, ByteString> responseMap, boolean isError);
}
