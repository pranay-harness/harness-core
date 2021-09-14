/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.entityactivity;

import io.harness.EntityType;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.entityactivity.connector.ConnectorEntityActivityEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class EntityActivityEventHandler {
  @Inject ConnectorEntityActivityEventHandler connectorEntityActivityEventHandler;

  public void updateActivityResultInEntity(NGActivityDTO ngActivityDTO) {
    // If in future, we need to update the activity for other entities like
    // secret, services, then we can change this if else to factory
    if (ngActivityDTO.getReferredEntity().getType() == EntityType.CONNECTORS) {
      connectorEntityActivityEventHandler.updateActivityResultInConnectors(ngActivityDTO);
    }
  }
}
