package io.harness.data;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.time.Duration.ofDays;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.pms.contracts.ambiance.Level;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(CDC)
@Value
@Builder
@Entity(value = "executionSweepingOutput", noClassnameStored = true)
@Document("executionSweepingOutput")
@FieldNameConstants(innerTypeName = "ExecutionSweepingOutputKeys")
@TypeAlias("executionSweepingOutput")
@StoreIn(DbAliases.PMS)
public class ExecutionSweepingOutputInstance implements PersistentEntity, UuidAccess {
  public static final Duration TTL = ofDays(30);

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_levelRuntimeIdUniqueIdx2")
                 .unique(true)
                 .field(ExecutionSweepingOutputKeys.planExecutionId)
                 .field(ExecutionSweepingOutputKeys.levelRuntimeIdIdx)
                 .field(ExecutionSweepingOutputKeys.name)
                 .build())
        .build();
  }
  @Wither @Id @org.mongodb.morphia.annotations.Id String uuid;
  @NotNull String planExecutionId;
  @Singular List<Level> levels;
  @NotNull @Trimmed String name;
  String levelRuntimeIdIdx;

  @Getter org.bson.Document value;
  @Wither @CreatedDate Long createdAt;

  @FdIndex @Builder.Default Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());

  @Wither @Version Long version;
}
