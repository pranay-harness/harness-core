package software.wings.graphql.schema.type.audit;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Scope(PermissionAttribute.ResourceType.APPLICATION)
public interface QLChangeSet extends QLObject {
  String getId();
  List<QLChangeDetails> getChanges();
  Long getTriggeredAt();
  QLRequestInfo getRequest();
  String getFailureStatusMsg();
}
