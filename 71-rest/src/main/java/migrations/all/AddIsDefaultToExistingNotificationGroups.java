package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.NotificationGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.NotificationSetupService;

import java.util.List;

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
    logger.info("Retrieving notificationGroups");
    PageResponse<NotificationGroup> pageResponse =
        wingsPersistence.query(NotificationGroup.class, pageRequest, excludeAuthority);

    List<NotificationGroup> notificationGroups = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(notificationGroups)) {
      logger.info("No NotificationGroups found");
      return;
    }

    for (NotificationGroup notificationGroup : notificationGroups) {
      // This will handle scenario, where isDefault is missing or is set to false
      // It might happen that before migration runs, some notification group has been marked as default
      // so we dont want to mark all as false.
      if (notificationGroup.isEditable() && !notificationGroup.isDefaultNotificationGroupForAccount()) {
        notificationGroup.setDefaultNotificationGroupForAccount(false);
        logger.info("... Updating notificationGroup, Id:[{}], Name:[{}]", notificationGroup.getUuid(),
            notificationGroup.getName());
        notificationSetupService.updateNotificationGroup(notificationGroup);
      }
    }
  }
}
