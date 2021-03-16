package io.harness.pms.approval.entities;

import io.harness.data.validator.Trimmed;
import io.harness.ng.core.NGAccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.pms.approval.beans.ApprovalStatus;
import io.harness.pms.approval.beans.ApprovalType;
import io.harness.pms.contracts.ambiance.Ambiance;

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
@Document("approvalInstances")
@Entity(value = "approvalInstances", noClassnameStored = true)
@Persistent
public class ApprovalInstance implements PersistentEntity, NGAccountAccess {
  @Id @org.mongodb.morphia.annotations.Id String id;

  @Trimmed @NotEmpty String accountIdentifier;
  @Trimmed @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @NotEmpty private String pipelineIdentifier;
  @NotNull private Ambiance ambiance;

  @NotNull ApprovalType type;
  @NotNull ApprovalStatus status;
  String approvalMessage;
  boolean includePipelineExecutionHistory;
  long deadline;

  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;
}
