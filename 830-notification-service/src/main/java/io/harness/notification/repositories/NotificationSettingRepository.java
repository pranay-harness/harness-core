/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
