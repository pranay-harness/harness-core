package io.harness.beans.stepDetail;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.pms.data.OrchestrationMap;

import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PIPELINE)
@Data
@Builder
@FieldNameConstants(innerTypeName = "StepDetailInstanceKeys")
@Entity(value = "stepDetailInstance", noClassnameStored = true)
@Document("stepDetailInstance")
@TypeAlias("stepDetailInstance")
@StoreIn(DbAliases.PMS)
public class StepDetailInstance {
  public static final long TTL_MONTHS = 6;

  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String name;
  String planExecutionId;
  String nodeExecutionId;
  OrchestrationMap stepDetails;

  @Builder.Default @FdTtlIndex Date validUntil = Date.from(OffsetDateTime.now().plusMonths(TTL_MONTHS).toInstant());

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("nodeExecutionId_name_unique_idx")
                 .field(StepDetailInstanceKeys.nodeExecutionId)
                 .field(StepDetailInstanceKeys.name)
                 .unique(true)
                 .build())
        .build();
  }
}
