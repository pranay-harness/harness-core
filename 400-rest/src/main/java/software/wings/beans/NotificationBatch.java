/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.annotation.HarnessEntity;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

/**
 * Created by anubhaw on 4/13/17.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "notificationBatch", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class NotificationBatch extends Base {
  private String batchId;
  private NotificationRule notificationRule;
  @Reference(idOnly = true, ignoreMissing = true) private List<Notification> notifications = new ArrayList<>();
}
