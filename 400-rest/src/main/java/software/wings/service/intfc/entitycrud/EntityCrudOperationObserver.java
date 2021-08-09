package software.wings.service.intfc.entitycrud;

import software.wings.beans.Event.Type;

public interface EntityCrudOperationObserver {
  <T> void handleEntityCrudOperation(String accountId, T OldEntity, T newEntity, Type type);
}
