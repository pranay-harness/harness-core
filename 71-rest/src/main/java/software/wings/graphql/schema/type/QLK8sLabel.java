package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLK8sLabelKeys")
@Scope(PermissionAttribute.ResourceType.K8S_LABEL)
public class QLK8sLabel {
  String name;
  String[] values;
}