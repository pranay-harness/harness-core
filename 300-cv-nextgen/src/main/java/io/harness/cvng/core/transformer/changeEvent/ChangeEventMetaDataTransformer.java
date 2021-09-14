/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeEventMetadata;
import io.harness.cvng.beans.change.ChangeSourceType;

public abstract class ChangeEventMetaDataTransformer<E extends Activity, M extends ChangeEventMetadata> {
  public abstract E getEntity(ChangeEventDTO changeEventDTO);

  public final ChangeEventDTO getDTO(E activity) {
    return ChangeEventDTO.builder()
        .accountId(activity.getAccountId())
        .orgIdentifier(activity.getOrgIdentifier())
        .projectIdentifier(activity.getProjectIdentifier())
        .serviceIdentifier(activity.getServiceIdentifier())
        .changeSourceIdentifier(activity.getChangeSourceIdentifier())
        .envIdentifier(activity.getEnvironmentIdentifier())
        .eventTime(activity.getEventTime().toEpochMilli())
        .type(ChangeSourceType.ofActivityType(activity.getType()))
        .changeEventMetaData(getMetadata(activity))
        .build();
  }

  protected abstract M getMetadata(E activity);
}
