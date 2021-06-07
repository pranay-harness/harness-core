package software.wings.graphql.datafetcher.hackathon;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLRecommendationDataPoint {
  String resource;
  String recommendationSubtype;
  String location;
  String region;
  String resourceType;
  String description;
  String insight;
  String resourceLink;
  Double cost;
  String currency;
  String costPeriod;
  String projectId;
  long lastUpdatedAt;
}
