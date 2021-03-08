package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
  @Id private String uuid;
  private String accountId;
  private DelegateInsightsType insightsType;
  private long periodStartTime;
  private long count;
  private String delegateGroupId;
}
