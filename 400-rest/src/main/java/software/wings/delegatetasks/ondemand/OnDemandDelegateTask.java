package software.wings.delegatetasks.ondemand;

import io.harness.mongo.index.FdIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "OnDemandDelegateTaskKeys")
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "onDemandDelegateTasks")
public class OnDemandDelegateTask implements PersistentEntity, CreatedAtAware, UpdatedAtAware, UuidAware {
  String accountId;
  @Id String uuid;
  @FdIndex private long createdAt;
  @FdIndex private long lastUpdatedAt;
  OnDemandTaskType type;
  String kubeConfig;
  String delegateYaml;
  OnDemandExecStatus status;

  public enum OnDemandExecStatus { QUEUED, PROCESSED, COMPLETED, FAILED }
  public enum OnDemandTaskType { CREATE, DESTROY }
}
