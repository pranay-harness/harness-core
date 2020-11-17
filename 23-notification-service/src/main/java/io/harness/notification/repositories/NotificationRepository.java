package io.harness.notification.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.entities.Notification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

import static io.harness.annotations.dev.HarnessTeam.PL;

@HarnessRepo
@OwnedBy(PL)
public interface NotificationRepository extends PagingAndSortingRepository<Notification, String> {
  Optional<Notification> findDistinctById(String id);
}
