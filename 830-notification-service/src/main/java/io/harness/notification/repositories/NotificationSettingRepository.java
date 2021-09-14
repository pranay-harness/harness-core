/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.notification.entities.NotificationSetting;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface NotificationSettingRepository extends PagingAndSortingRepository<NotificationSetting, String> {
  Optional<NotificationSetting> findByAccountId(String accountId);
}
