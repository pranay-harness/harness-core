package io.harness.delegate.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateEntityOwner.DelegateEntityOwnerKeys;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@FieldNameConstants(innerTypeName = "DelegateGroupKeys")
@Entity(value = "delegateGroups", noClassnameStored = true)
@HarnessEntity(exportable = true)
@OwnedBy(HarnessTeam.DEL)
public class DelegateGroup implements PersistentEntity, UuidAware {
  @Id @NotNull private String uuid;

  @NotEmpty private String name;

  private String delegateType;

  private String description;

  @NotEmpty private String accountId;

  // Will be used for NG to hold information about delegate group if it is owned at Org / Project
  private DelegateEntityOwner owner;

  // Will be used for segregation of CG vs. NG delegate groups.
  private boolean ng;

  private String delegateConfigurationId;

  private DelegateSizeDetails sizeDetails;

  private K8sConfigDetails k8sConfigDetails;

  @Builder.Default private DelegateGroupStatus status = DelegateGroupStatus.ENABLED;

  @FdTtlIndex private Date validUntil;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("byAccount")
                 .unique(true)
                 .field(DelegateGroupKeys.accountId)
                 .field(DelegateGroupKeys.name)
                 .field(DelegateGroupKeys.ng)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .field(DelegateGroupKeys.accountId)
                 .field(DelegateGroupKeys.ng)
                 .field(DelegateGroupKeys.owner)
                 .name("byAcctNgOwner")
                 .build())
        .build();
  }

  @UtilityClass
  public static final class DelegateGroupKeys {
    public static final String owner_identifier = owner + "." + DelegateEntityOwnerKeys.identifier;
  }
}
