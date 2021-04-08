package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.notification.templates.PredefinedTemplate;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Singleton
@Slf4j
public class NotificationTemplateRegistrar implements Runnable {
  @Inject NotificationClient notificationClient;

  @Override
  public void run() {
    try {
      int timout = 1;
      List<PredefinedTemplate> templates = new ArrayList<>(Arrays.asList(PredefinedTemplate.PIPELINE_PLAIN_SLACK,
          PredefinedTemplate.PIPELINE_PLAIN_EMAIL, PredefinedTemplate.PIPELINE_PLAIN_PAGERDUTY,
          PredefinedTemplate.PIPELINE_PLAIN_MSTEAMS, PredefinedTemplate.STAGE_PLAIN_SLACK,
          PredefinedTemplate.STAGE_PLAIN_EMAIL, PredefinedTemplate.STAGE_PLAIN_PAGERDUTY,
          PredefinedTemplate.STAGE_PLAIN_MSTEAMS, PredefinedTemplate.STEP_PLAIN_EMAIL,
          PredefinedTemplate.STEP_PLAIN_SLACK, PredefinedTemplate.STEP_PLAIN_MSTEAMS,
          PredefinedTemplate.STEP_PLAIN_PAGERDUTY, PredefinedTemplate.HARNESS_APPROVAL_NOTIFICATION_SLACK,
          PredefinedTemplate.HARNESS_APPROVAL_NOTIFICATION_EMAIL));
      while (true) {
        List<PredefinedTemplate> unprocessedTemplate = new ArrayList<>();
        for (PredefinedTemplate template : templates) {
          log.info("Registering {} with NotificationService", template);
          try {
            notificationClient.saveNotificationTemplate(Team.PIPELINE, template, true);
          } catch (Exception ex) {
            unprocessedTemplate.add(template);
          }
        }
        if (unprocessedTemplate.isEmpty()) {
          break;
        }

        Thread.sleep(timout);

        timout *= 10;
        templates = unprocessedTemplate;
      }
    } catch (InterruptedException e) {
      log.error("", e);
    }
  }
}
