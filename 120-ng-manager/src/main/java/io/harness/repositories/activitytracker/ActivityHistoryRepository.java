package io.harness.repositories.activitytracker;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.activitytracker.entities.ActivityHistory;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
public interface ActivityHistoryRepository
    extends CrudRepository<ActivityHistory, String>, ActivityHistoryRepositoryCustom {
  List<ActivityHistory> findByProjectIdAndCreatedAtBetween(String projectId, long startTime, long endTime);

  List<ActivityHistory> findByUserIdAndCreatedAtBetween(String userId, long startTime, long endTime);

  List<ActivityHistory> findByProjectIdAndUserIdAndCreatedAtBetween(
      String projectId, String userId, long startTime, long endTime);

  List<ActivityHistory> findByProjectIdAndCreatedAtBetweenOrderByCreatedAtDesc(
      String projectId, long startTime, long endTime);

  List<ActivityHistory> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
      String userId, long startTime, long endTime);

  List<ActivityHistory> findByProjectIdAndUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
      String projectId, String userId, long startTime, long endTime);
}
