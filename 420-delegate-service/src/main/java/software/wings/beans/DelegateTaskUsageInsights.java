package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@FieldNameConstants(innerTypeName = "DelegateTaskUsageInsightsKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Entity(value = "delegateTaskUsageInsights", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class DelegateTaskUsageInsights implements PersistentEntity, UuidAware {
  @Id private String uuid;
  @FdIndex private long timestamp;
  private String accountId;
  private String taskId;
  private DelegateTaskUsageInsightsEventType eventType;
  private String delegateId;
  private String delegateGroup;
}
