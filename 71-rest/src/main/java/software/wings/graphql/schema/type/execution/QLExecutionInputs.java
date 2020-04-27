package software.wings.graphql.schema.type.execution;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLService;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLApplicationKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
public class QLExecutionInputs {
  List<QLService> serviceInputs;
}
