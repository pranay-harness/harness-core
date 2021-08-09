package software.wings.graphql.schema.query;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.NameValuePair;

import java.util.List;
import lombok.Value;

@Value
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CDC)
public class QLApproveOrRejectApprovalsInput {
  String executionId;
  String approvalId;
  Action action;
  List<NameValuePair> variableInputs;
  String applicationId;
  String comments;
}
