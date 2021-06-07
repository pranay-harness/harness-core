package io.harness.ng.activitytracker.models;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ActivityHistoryDetails {
  long timestamp;
  String projectId;
  String projectName;
  String resourceId;
  String resourceName;
  ResourceType resourceType;
  ActivityType activityType;
  String userId;
  String userName;
  String userEmail;
}
