package io.harness.notification.repositories;

import io.harness.Team;
import io.harness.annotation.HarnessRepo;
import io.harness.notification.entities.NotificationTemplate;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface NotificationTemplateRepository extends PagingAndSortingRepository<NotificationTemplate, String> {
  List<NotificationTemplate> findByTeam(Team team);

  Optional<NotificationTemplate> findByIdentifierAndTeam(String identifier, Team team);

  void deleteByTeam(Team team);
}
