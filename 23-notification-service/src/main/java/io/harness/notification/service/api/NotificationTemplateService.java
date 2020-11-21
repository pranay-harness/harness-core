package io.harness.notification.service.api;

import io.harness.Team;
import io.harness.notification.entities.NotificationTemplate;
import io.harness.stream.BoundedInputStream;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public interface NotificationTemplateService {
  NotificationTemplate create(String identifier, Team team, BoundedInputStream inputStream);

  NotificationTemplate save(@NotNull NotificationTemplate notificationTemplate);

  Optional<NotificationTemplate> update(String templateIdentifier, Team team, BoundedInputStream inputStream);

  List<NotificationTemplate> list(Team team);

  Optional<NotificationTemplate> getByIdentifierAndTeam(String identifier, Team team);

  Optional<String> getTemplateAsString(String identifier, Team team);

  Optional<String> getTemplateAsString(String identifier);

  boolean delete(String templateIdentifier, Team team);

  void dropPredefinedTemplates();
}
