/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.mutation.approval.input;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.NameValuePair;

import java.util.List;
import lombok.Value;

@Value
@OwnedBy(CDC)
public class QLApproveOrRejectApprovalsInput {
  String executionId;
  String approvalId;
  Action action;
  List<NameValuePair> variableInputs;
  String applicationId;
  String comments;
  String clientMutationId;
}
