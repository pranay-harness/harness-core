package software.wings.graphql.schema.type.audit;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Scope(PermissionAttribute.ResourceType.APPLICATION)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface QLChangeSet extends QLObject {
  String getId();
  List<QLChangeDetails> getChanges();
  Long getTriggeredAt();
  QLRequestInfo getRequest();
  String getFailureStatusMsg();
}
