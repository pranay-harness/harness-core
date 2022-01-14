package software.wings.graphql.schema.type.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.approval.ConditionalOperator;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCriteriaKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
@OwnedBy(CDC)
public class QLCriteria {
  ConditionalOperator operator;
  List<QLCondition> conditions;
}
