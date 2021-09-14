/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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

  Optional<NotificationTemplate> findByIdentifierAndTeamExists(String identifier, boolean teamExists);

  void deleteByTeam(Team team);
}
