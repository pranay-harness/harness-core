package io.harness.pms.notification;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.notification.bean.NotificationRules;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class NotificationInstrumentationHelper {
  @Inject NotificationHelper notificationHelper;

  public List<NotificationRules> getNotificationMethods(String planExecutionId) {
    String yaml = notificationHelper.obtainYaml(planExecutionId);
    List<NotificationRules> notificationRules = new ArrayList<>();
    if (EmptyPredicate.isEmpty(yaml)) {
      log.error("Empty yaml found in executionMetaData");
      return notificationRules;
    }
    try {
      notificationRules = notificationHelper.getNotificationRulesFromYaml(yaml);
    } catch (IOException exception) {
      log.error("Unable to parse yaml to get notification objects", exception);
    }
    return notificationRules != null ? notificationRules : Collections.emptyList();
  }

  public Set<String> getNotificationMethodTypes(List<NotificationRules> notificationRules) {
    return notificationRules.stream().map(o -> o.getNotificationChannelWrapper().getType()).collect(Collectors.toSet());
  }
}
