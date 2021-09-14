/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.entitycrud;

import software.wings.beans.Event.Type;

public interface EntityCrudOperationObserver {
  <T> void handleEntityCrudOperation(String accountId, T OldEntity, T newEntity, Type type);
}
