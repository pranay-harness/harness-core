/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.migrations.Migration;

import software.wings.beans.NotificationGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.NotificationSetupService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddIsDefaultToExistingNotificationGroups implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private NotificationSetupService notificationSetupService;

  /**
   * Add "isDefault = false" for all existing notification groups
   */
  @Override
  public void migrate() {
    PageRequest<NotificationGroup> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    log.info("Retrieving notificationGroups");
    PageResponse<NotificationGroup> pageResponse =
        wingsPersistence.query(NotificationGroup.class, pageRequest, excludeAuthority);

    List<NotificationGroup> notificationGroups = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(notificationGroups)) {
      log.info("No NotificationGroups found");
      return;
    }

    for (NotificationGroup notificationGroup : notificationGroups) {
      // This will handle scenario, where isDefault is missing or is set to false
      // It might happen that before migration runs, some notification group has been marked as default
      // so we dont want to mark all as false.
      if (notificationGroup.isEditable() && !notificationGroup.isDefaultNotificationGroupForAccount()) {
        notificationGroup.setDefaultNotificationGroupForAccount(false);
        log.info("... Updating notificationGroup, Id:[{}], Name:[{}]", notificationGroup.getUuid(),
            notificationGroup.getName());
        notificationSetupService.updateNotificationGroup(notificationGroup);
      }
    }
  }
}
