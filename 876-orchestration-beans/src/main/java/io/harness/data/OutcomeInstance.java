package io.harness.data;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.data.PmsOutcome;

import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PIPELINE)
@Value
@Builder
@Entity(value = "outcomeInstances", noClassnameStored = true)
@Document("outcomeInstances")
@FieldNameConstants(innerTypeName = "OutcomeInstanceKeys")
@TypeAlias("outcomeInstance")
@StoreIn(DbAliases.PMS)
public class OutcomeInstance implements PersistentEntity, UuidAccess {
  public static final long TTL_MONTHS = 6;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_producedBySetupIdRuntimeIdIdx")
                 .unique(true)
                 .field(OutcomeInstanceKeys.planExecutionId)
                 .field("producedBy.setupId")
                 .field("producedBy.runtimeId")
                 .field(OutcomeInstanceKeys.name)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("unique_levelRuntimeIdUniqueIdx")
                 .unique(true)
                 .field(OutcomeInstanceKeys.planExecutionId)
                 .field(OutcomeInstanceKeys.levelRuntimeIdIdx)
                 .field(OutcomeInstanceKeys.name)
                 .build())
        .build();
  }

  @Wither @Id @org.mongodb.morphia.annotations.Id String uuid;
  @NonNull String planExecutionId;
  String stageExecutionId;
  Level producedBy;
  @NotEmpty @Trimmed String name;
  String levelRuntimeIdIdx;
  @Deprecated org.bson.Document outcome;
  PmsOutcome outcomeValue;
  @Wither @CreatedDate Long createdAt;
  @Wither @Version Long version;
  String groupName;
  @Builder.Default @FdTtlIndex Date validUntil = Date.from(OffsetDateTime.now().plusMonths(TTL_MONTHS).toInstant());

  public String getOutcomeJsonValue() {
    if (!isEmpty(outcomeValue)) {
      return outcomeValue.toJson();
    }

    if (!isEmpty(outcome)) {
      return outcome.toJson();
    }

    return null;
  }

  @UtilityClass
  public static class OutcomeInstanceKeys {
    public final String producedBySetupId = OutcomeInstanceKeys.producedBy + ".setupId";
    public final String producedByRuntimeId = OutcomeInstanceKeys.producedBy + ".runtimeId";
  }
}
