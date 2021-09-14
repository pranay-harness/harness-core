/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLExecutionStatus;

import graphql.schema.DataFetchingFieldSelectionSet;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Value;

@Value
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
public class QLExecutionsQueryParameters implements QLPageQueryParameters {
  private String applicationId;
  private String pipelineId;
  private String workflowId;
  private String serviceId;

  private OffsetDateTime from;
  private OffsetDateTime to;

  private List<QLExecutionStatus> statuses;

  private int limit;
  private int offset;

  private DataFetchingFieldSelectionSet selectionSet;
}
