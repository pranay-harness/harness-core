/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLExecutionStatus;

import graphql.schema.DataFetchingFieldSelectionSet;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Value;

@Value
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
