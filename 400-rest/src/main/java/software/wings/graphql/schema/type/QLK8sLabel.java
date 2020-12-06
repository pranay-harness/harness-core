package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLK8sLabelKeys")
@Scope(PermissionAttribute.ResourceType.K8S_LABEL)
public class QLK8sLabel {
  String name;
  String[] values;
}
