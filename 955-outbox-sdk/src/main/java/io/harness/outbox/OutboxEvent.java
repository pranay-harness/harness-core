package io.harness.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContext;
import io.harness.iterator.PersistentIterable;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.core.Resource;
import io.harness.ng.core.Resource.ResourceKeys;
import io.harness.ng.core.ResourceScope;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PL)
@Getter
@Builder
@FieldNameConstants(innerTypeName = "OutboxEventKeys")
@Entity(value = "outboxEvents", noClassnameStored = true)
@Document("outboxEvents")
@TypeAlias("outboxEvents")
public class OutboxEvent implements PersistentIterable, PersistentRegularIterable {
  @Id @org.mongodb.morphia.annotations.Id String id;

  @NotNull ResourceScope resourceScope;
  @NotNull @Valid Resource resource;

  @NotNull String eventType;
  @NotNull String eventData;

  @CreatedDate Long createdAt;
  @Setter @Builder.Default Boolean blocked = Boolean.FALSE;
  @Setter Instant nextUnblockAttemptAt;

  GlobalContext globalContext;

  Long nextIteration;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("createdAt_blocked_outbox_Idx")
                 .field(OutboxEventKeys.createdAt)
                 .field(OutboxEventKeys.blocked)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("resourceType_createdAt_outbox_Idx")
                 .field(OutboxEventKeys.RESOURCE_TYPE_KEY)
                 .field(OutboxEventKeys.createdAt)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("eventType_createdAt_outbox_Idx")
                 .field(OutboxEventKeys.eventType)
                 .field(OutboxEventKeys.createdAt)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("blocked_nextIteration_createdAt_outbox_Idx")
                 .field(OutboxEventKeys.blocked)
                 .field(OutboxEventKeys.nextIteration)
                 .field(OutboxEventKeys.createdAt)
                 .build())
        .build();
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return this.nextIteration;
  }

  @Override
  public String getUuid() {
    return this.id;
  }

  @UtilityClass
  public static final class OutboxEventKeys {
    public static final String RESOURCE_TYPE_KEY = OutboxEventKeys.resource + "." + ResourceKeys.type;
  }
}
