/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.service.api;

import io.harness.Team;
import io.harness.notification.entities.NotificationTemplate;
import io.harness.stream.BoundedInputStream;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public interface NotificationTemplateService {
  NotificationTemplate create(String identifier, Team team, BoundedInputStream inputStream, Boolean harnessManaged);

  NotificationTemplate save(@NotNull NotificationTemplate notificationTemplate);

  Optional<NotificationTemplate> update(
      String templateIdentifier, Team team, BoundedInputStream inputStream, Boolean harnessManaged);

  List<NotificationTemplate> list(Team team);

  Optional<NotificationTemplate> getByIdentifierAndTeam(String identifier, Team team);

  Optional<String> getTemplateAsString(String identifier, Team team);

  Optional<NotificationTemplate> getPredefinedTemplate(String identifier);

  boolean delete(String templateIdentifier, Team team);

  void dropPredefinedTemplates();
}
