package software.wings.graphql.schema.type.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLService;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLApplicationKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
public class QLExecutionInputs {
  List<QLService> serviceInputs;
}
