package io.harness.notification.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.Team;
import io.harness.notification.exception.NotificationException;
import io.harness.notification.entities.NotificationTemplate;
import io.harness.notification.repositories.NotificationTemplateRepository;
import io.harness.notification.service.api.NotificationTemplateService;
import io.harness.stream.BoundedInputStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.google.common.io.ByteStreams.toByteArray;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.exception.WingsException.USER;

@Slf4j
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NotificationTemplateServiceImpl implements NotificationTemplateService {
  private final NotificationTemplateRepository notificationTemplateRepository;

  @Override
  public NotificationTemplate create(
      @NotNull String identifier, @NotNull Team team, @NotNull BoundedInputStream inputStream) {
    NotificationTemplate template = NotificationTemplate.builder().build();
    template.setTeam(team);
    template.setIdentifier(identifier);
    try {
      template.setFile(toByteArray(inputStream));
    } catch (Exception ex) {
      log.error("Exception while converting file to byte array.");
      throw new NotificationException("IO error", DEFAULT_ERROR_CODE, USER);
    }
    return notificationTemplateRepository.save(template);
  }

  @Override
  public NotificationTemplate save(NotificationTemplate notificationTemplate) {
    return notificationTemplateRepository.save(notificationTemplate);
  }

  @Override
  public Optional<NotificationTemplate> update(
      @NotNull String templateIdentifier, Team team, BoundedInputStream inputStream) {
    Optional<NotificationTemplate> templateOptional = getByIdentifierAndTeam(templateIdentifier, team);
    if (templateOptional.isPresent()) {
      NotificationTemplate template = templateOptional.get();
      try {
        template.setFile(toByteArray(inputStream));
        return Optional.of(notificationTemplateRepository.save(template));
      } catch (IOException e) {
        log.error("Error while converting input stream to byte array", e);
      }
    }
    return Optional.empty();
  }

  @Override
  public List<NotificationTemplate> list(Team team) {
    return notificationTemplateRepository.findByTeam(team);
  }

  @Override
  public Optional<NotificationTemplate> getByIdentifierAndTeam(String identifier, Team team) {
    return notificationTemplateRepository.findByIdentifierAndTeam(identifier, team);
  }

  @Override
  public Optional<String> getTemplateAsString(String identifier, Team team) {
    Optional<NotificationTemplate> templateOptional = getByIdentifierAndTeam(identifier, team);
    if (templateOptional.isPresent()) {
      NotificationTemplate template = templateOptional.get();
      return Optional.of(new String(template.getFile()));
    }
    return Optional.empty();
  }

  @Override
  public Optional<String> getTemplateAsString(String identifier) {
    return getTemplateAsString(identifier, null);
  }

  @Override
  public boolean delete(String templateIdentifier, Team team) {
    Optional<NotificationTemplate> templateOptional = getByIdentifierAndTeam(templateIdentifier, team);
    templateOptional.ifPresent(notificationTemplateRepository::delete);
    return true;
  }

  @Override
  public void dropPredefinedTemplates() {
    notificationTemplateRepository.deleteByTeam(null);
  }
}
