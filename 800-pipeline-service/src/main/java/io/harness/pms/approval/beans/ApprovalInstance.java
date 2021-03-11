package io.harness.pms.approval.beans;

import io.harness.data.validator.Trimmed;
import io.harness.ng.core.NGAccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.pms.contracts.ambiance.Ambiance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@FieldNameConstants(innerTypeName = "ApprovalInstanceKeys")
@Entity(value = "approvalInstances", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("approvalInstances")
@Persistent
public class ApprovalInstance implements PersistentEntity, NGAccountAccess {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @Trimmed @NotEmpty String accountIdentifier;
  @Trimmed String orgIdentifier;
  @Trimmed String projectIdentifier;
  @NotNull private String pipelineId;
  @NotNull private Ambiance ambiance;
  @NotEmpty ApprovalType type;
  // Will be set if approval has reached it final state.
  long deadline;
  ApprovalStatus status;
  String approvalMessage;
  boolean includePipelineExecutionHistory;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;
}
