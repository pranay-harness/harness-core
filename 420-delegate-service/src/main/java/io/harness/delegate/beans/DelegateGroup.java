package io.harness.delegate.beans;

import io.harness.annotation.HarnessEntity;
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
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@FieldNameConstants(innerTypeName = "DelegateGroupKeys")
@Entity(value = "delegateGroups", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class DelegateGroup implements PersistentEntity, UuidAware {
  @Id @NotNull private String uuid;

  @NotEmpty private String name;

  @NotEmpty private String accountId;

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
                 .build())
        .build();
  }
}
