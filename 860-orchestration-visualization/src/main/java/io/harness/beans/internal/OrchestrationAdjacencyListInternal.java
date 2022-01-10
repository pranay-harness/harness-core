/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans.internal;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.GraphVertex;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class OrchestrationAdjacencyListInternal {
  Map<String, GraphVertex> graphVertexMap;
  Map<String, EdgeListInternal> adjacencyMap;
}
