package io.harness.notification.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.notification.entities.NotificationSetting;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface NotificationSettingRepository extends PagingAndSortingRepository<NotificationSetting, String> {
  Optional<NotificationSetting> findByAccountId(String accountId);
}
