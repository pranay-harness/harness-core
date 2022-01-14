package io.harness.notification.repositories;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.entities.Notification;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface NotificationRepository
    extends PagingAndSortingRepository<Notification, String>, NotificationRepositoryCustom {
  Optional<Notification> findDistinctById(String id);
}
