package io.harness.ng.activitytracker.entities;

import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.activitytracker.models.ActivityType;
import io.harness.ng.activitytracker.models.ResourceType;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "activityHistoryKeys")
@Entity(value = "activityHistory", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("activityHistory")
public class ActivityHistory implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("projectId_userId_createdAt")
                 .unique(true)
                 .field(activityHistoryKeys.projectId)
                 .field(activityHistoryKeys.userId)
                 .field(activityHistoryKeys.createdAt)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("projectId_createdAt")
                 .unique(true)
                 .field(activityHistoryKeys.projectId)
                 .field(activityHistoryKeys.createdAt)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("userId_createdAt")
                 .unique(true)
                 .field(activityHistoryKeys.userId)
                 .field(activityHistoryKeys.createdAt)
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id String id;
  String projectId;
  String projectName;
  String resourceId;
  String resourceName;
  ResourceType resourceType;
  ActivityType activityType;
  String userId;
  String userName;
  String userEmail;
  @CreatedDate Long createdAt;
}
