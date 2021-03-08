package io.harness.outbox;

import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.core.ResourceScope;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@FieldNameConstants(innerTypeName = "OutboxEventKeys")
@Entity(value = "outboxEvents", noClassnameStored = true)
@Document("outboxEvents")
public class OutboxEvent {
  @Id @org.mongodb.morphia.annotations.Id String id;

  @NotNull ResourceScope resourceScope;
  @NotNull String resourceIdentifier;
  @NotNull String resourceType;

  @NotNull String eventType;
  @NotNull JsonNode eventData;

  @CreatedDate Long createdAt;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("createdAt_entityType_Idx")
                 .field(OutboxEventKeys.createdAt)
                 .field(OutboxEventKeys.eventType)
                 .build())
        .build();
  }
}
