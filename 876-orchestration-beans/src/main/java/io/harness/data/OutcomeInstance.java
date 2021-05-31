package io.harness.data;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.pms.contracts.ambiance.Level;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
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
  @NotEmpty String planExecutionId;
  @Singular List<Level> levels;
  Level producedBy;
  @NotEmpty @Trimmed String name;
  String levelRuntimeIdIdx;
  org.bson.Document outcome;
  @Wither @CreatedDate Long createdAt;
  @Wither @Version Long version;

  @UtilityClass
  public static class OutcomeInstanceKeys {
    public final String producedBySetupId = OutcomeInstanceKeys.producedBy + ".setupId";
    public final String producedByRuntimeId = OutcomeInstanceKeys.producedBy + ".runtimeId";
  }
}
