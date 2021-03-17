package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@FieldNameConstants(innerTypeName = "DelegateInsightsSummaryKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Entity(value = "delegateInsightsSummary", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class DelegateInsightsSummary implements PersistentEntity, UuidAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("byAcctGrpTime")
                 .field(DelegateInsightsSummaryKeys.accountId)
                 .field(DelegateInsightsSummaryKeys.delegateGroupId)
                 .field(DelegateInsightsSummaryKeys.periodStartTime)
                 .build())
        .build();
  }

  @Id private String uuid;
  private String accountId;
  private DelegateInsightsType insightsType;
  private long periodStartTime;
  private long count;
  private String delegateGroupId;
}
