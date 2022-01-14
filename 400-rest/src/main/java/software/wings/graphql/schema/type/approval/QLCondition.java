package software.wings.graphql.schema.type.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLConditionKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CDC)
public class QLCondition {
  String conditionKey;
  List<String> conditionValues;
}
