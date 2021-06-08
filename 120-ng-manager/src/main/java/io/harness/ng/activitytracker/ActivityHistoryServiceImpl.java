package io.harness.ng.activitytracker;

import io.harness.NGDateUtils;
import io.harness.ng.activitytracker.entities.ActivityHistory;
import io.harness.ng.activitytracker.models.ActivityHistoryByProject;
import io.harness.ng.activitytracker.models.ActivityHistoryByUser;
import io.harness.ng.activitytracker.models.ActivityHistoryDetails;
import io.harness.ng.activitytracker.models.ActivityStatsPerTimestamp;
import io.harness.ng.activitytracker.models.ActivityType;
import io.harness.ng.activitytracker.models.CountPerActivityType;
import io.harness.ng.activitytracker.models.apiresponses.ActivityHistoryDetailsResponse;
import io.harness.ng.activitytracker.models.apiresponses.StatsDetailsByProjectResponse;
import io.harness.ng.activitytracker.models.apiresponses.StatsDetailsByUserResponse;
import io.harness.ng.activitytracker.models.apiresponses.StatsDetailsResponse;
import io.harness.repositories.activitytracker.ActivityHistoryRepository;

import com.google.api.client.util.ArrayMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ActivityHistoryServiceImpl implements ActivityHistoryService {
  @Inject private ActivityHistoryRepository activityHistoryRepository;

  public StatsDetailsResponse getStatsDetails(String projectId, String userId, long startTime, long endTime) {
    List<ActivityHistory> activityHistoryList;
    if (projectId != null && userId != null) {
      activityHistoryList =
          activityHistoryRepository.findByProjectIdAndUserIdAndCreatedAtBetween(projectId, userId, startTime, endTime);
    } else if (userId != null) {
      activityHistoryList = activityHistoryRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    } else {
      activityHistoryList = activityHistoryRepository.findByProjectIdAndCreatedAtBetween(projectId, startTime, endTime);
    }

    List<ActivityStatsPerTimestamp> activityStatsPerTimestampList = aggregateDataPerDay(activityHistoryList);

    return StatsDetailsResponse.builder().activityStatsPerTimestampList(activityStatsPerTimestampList).build();
  }

  public StatsDetailsByUserResponse getStatsDetailsByUsers(String projectId, long startTime, long endTime) {
    List<ActivityHistory> activityHistoryList =
        activityHistoryRepository.findByProjectIdAndCreatedAtBetween(projectId, startTime, endTime);

    List<ActivityHistoryByUser> activityHistoryByUserList = new ArrayList<>();
    Map<String, List<ActivityHistory>> userToActivityHistoryList = new ArrayMap<>();
    for (ActivityHistory activityHistory : activityHistoryList) {
      userToActivityHistoryList.putIfAbsent(activityHistory.getUserId(), new ArrayList<>());
      List<ActivityHistory> activityHistoryListByUser = userToActivityHistoryList.get(activityHistory.getUserId());
      activityHistoryListByUser.add(activityHistory);
    }

    userToActivityHistoryList.forEach((userId, activityList) -> {
      activityHistoryByUserList.add(ActivityHistoryByUser.builder()
                                        .activityStatsPerTimestampList(aggregateDataPerDay(activityHistoryList))
                                        .userId(userId)
                                        .build());
    });

    return StatsDetailsByUserResponse.builder().activityHistoryByUserList(activityHistoryByUserList).build();
  }

  public StatsDetailsByProjectResponse getStatsDetailsByProjects(String userId, long startTime, long endTime) {
    List<ActivityHistory> activityHistoryList =
        activityHistoryRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime);

    List<ActivityHistoryByProject> activityHistoryByProjectList = new ArrayList<>();
    Map<String, List<ActivityHistory>> userToActivityHistoryList = new ArrayMap<>();
    for (ActivityHistory activityHistory : activityHistoryList) {
      userToActivityHistoryList.putIfAbsent(activityHistory.getProjectId(), new ArrayList<>());
      List<ActivityHistory> activityHistoryListByProject =
          userToActivityHistoryList.get(activityHistory.getProjectId());
      activityHistoryListByProject.add(activityHistory);
    }

    userToActivityHistoryList.forEach((projectId, activityList) -> {
      activityHistoryByProjectList.add(ActivityHistoryByProject.builder()
                                           .activityStatsPerTimestampList(aggregateDataPerDay(activityHistoryList))
                                           .projectId(projectId)
                                           .build());
    });

    return StatsDetailsByProjectResponse.builder().activityHistoryByUserList(activityHistoryByProjectList).build();
  }

  public ActivityHistoryDetailsResponse getActivityHistoryDetails(
      String projectId, String userId, long startTime, long endTime) {
    List<ActivityHistory> activityHistoryList;
    if (projectId != null && userId != null) {
      activityHistoryList = activityHistoryRepository.findByProjectIdAndUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
          projectId, userId, startTime, endTime);
    } else if (userId != null) {
      activityHistoryList =
          activityHistoryRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startTime, endTime);
    } else {
      activityHistoryList = activityHistoryRepository.findByProjectIdAndCreatedAtBetweenOrderByCreatedAtDesc(
          projectId, startTime, endTime);
    }

    List<ActivityHistoryDetails> activityHistoryDetails = new ArrayList<>();
    for (ActivityHistory activityHistory : activityHistoryList) {
      activityHistoryDetails.add(ActivityHistoryDetails.builder()
                                     .activityType(activityHistory.getActivityType())
                                     .projectName(activityHistory.getProjectName())
                                     .resourceName(activityHistory.getResourceName())
                                     .resourceType(activityHistory.getResourceType())
                                     .userName(activityHistory.getUserName())
                                     .timestamp(activityHistory.getCreatedAt())
                                     .projectId(activityHistory.getProjectId())
                                     .resourceId(activityHistory.getResourceId())
                                     .userEmail(activityHistory.getUserEmail())
                                     .userId(activityHistory.getUserId())
                                     .build());
    }

    return ActivityHistoryDetailsResponse.builder().activityHistoryDetailsList(activityHistoryDetails).build();
  }

  // ------------------------------- PRIVATE METHODS -----------------------------

  List<ActivityStatsPerTimestamp> aggregateDataPerDay(List<ActivityHistory> activityHistoryList) {
    Map<Long, Map<ActivityType, Integer>> detailsPerTimestamp = new HashMap<>();
    Map<Long, Integer> totalCountPerTimestamp = new HashMap<>();

    for (ActivityHistory activityHistory : activityHistoryList) {
      ActivityType activityType = activityHistory.getActivityType();
      long dayTimestamp = NGDateUtils.getStartTimeOfTheDayAsEpoch(activityHistory.getCreatedAt());

      detailsPerTimestamp.putIfAbsent(dayTimestamp, new HashMap<>());
      Map<ActivityType, Integer> detailsPerActivityType = detailsPerTimestamp.get(dayTimestamp);
      int count = detailsPerActivityType.getOrDefault(activityType, 0);
      detailsPerActivityType.put(activityType, count + 1);
      detailsPerTimestamp.put(dayTimestamp, detailsPerActivityType);

      // update total count
      count = totalCountPerTimestamp.getOrDefault(dayTimestamp, 0);
      totalCountPerTimestamp.put(dayTimestamp, count + 1);
    }

    List<ActivityStatsPerTimestamp> response = new ArrayList<>();
    detailsPerTimestamp.forEach((k, v) -> {
      List<CountPerActivityType> countPerActivityTypeList = new ArrayList<>();
      v.forEach((activityType, value)
                    -> countPerActivityTypeList.add(
                        CountPerActivityType.builder().activityType(activityType).count(value).build()));

      response.add(ActivityStatsPerTimestamp.builder()
                       .countPerActivityTypeList(countPerActivityTypeList)
                       .timestamp(k)
                       .totalCount(totalCountPerTimestamp.get(k))
                       .build());
    });

    return response;
  }
}
