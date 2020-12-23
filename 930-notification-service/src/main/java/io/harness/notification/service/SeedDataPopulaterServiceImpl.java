package io.harness.notification.service;

import io.harness.notification.SeedDataConfiguration;
import io.harness.notification.entities.NotificationTemplate;
import io.harness.notification.service.api.NotificationTemplateService;
import io.harness.notification.service.api.SeedDataPopulaterService;
import io.harness.notification.templates.PredefinedTemplate;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URL;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class SeedDataPopulaterServiceImpl implements SeedDataPopulaterService {
  private final NotificationTemplateService notificationTemplateService;

  private void populatePredefinedTemplates(SeedDataConfiguration seedDataConfiguration) {
    if (seedDataConfiguration.shouldOverrideAllPredefinedTemplates()) {
      notificationTemplateService.dropPredefinedTemplates();
    }
    for (PredefinedTemplate predefinedTemplate : PredefinedTemplate.values()) {
      String identifier = predefinedTemplate.getIdentifier();
      String path = predefinedTemplate.getPath();

      if (notificationTemplateService.getByIdentifierAndTeam(identifier, null).isPresent()) {
        continue;
      }
      URL url = Resources.getResource(path);
      try {
        byte[] file = Resources.toByteArray(url);
        NotificationTemplate notificationTemplate =
            NotificationTemplate.builder().identifier(identifier).file(file).harnessManaged(true).build();
        log.info("Saved predefined template: {} to database", identifier);
        notificationTemplateService.save(notificationTemplate);
      } catch (IOException exception) {
        log.error("Error while converting file to byte array: {}", path);
      }
    }
  }

  @Override
  public void populateSeedData(SeedDataConfiguration seedDataConfiguration) {
    populatePredefinedTemplates(seedDataConfiguration);
  }
}
