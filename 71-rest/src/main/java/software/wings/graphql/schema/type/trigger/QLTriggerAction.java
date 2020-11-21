package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@OwnedBy(CDC)
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public interface QLTriggerAction extends QLObject {
  List<QLTriggerVariableValue> getVariables();
  List<QLArtifactSelection> getArtifactSelections();
}
