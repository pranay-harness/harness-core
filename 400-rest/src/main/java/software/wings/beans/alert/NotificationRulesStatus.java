/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;

import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Entity(value = "notificationRulesStatuses", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class NotificationRulesStatus implements PersistentEntity, AccountAccess {
  @Id private String accountId;
  private boolean enabled;
}
