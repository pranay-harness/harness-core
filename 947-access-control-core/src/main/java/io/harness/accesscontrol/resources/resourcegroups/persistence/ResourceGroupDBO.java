package io.harness.accesscontrol.resources.resourcegroups.persistence;

import static io.harness.ng.DbAliases.ACCESS_CONTROL;

import io.harness.accesscontrol.AccessControlEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.PL)
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@FieldNameConstants(innerTypeName = "ResourceGroupDBOKeys")
@Entity(value = "resourcegroups", noClassnameStored = true)
@Document("resourcegroups")
@TypeAlias("resourcegroups")
@StoreIn(ACCESS_CONTROL)
public class ResourceGroupDBO implements PersistentRegularIterable, AccessControlEntity {
  @Setter @Id @org.mongodb.morphia.annotations.Id String id;
  @NotEmpty final String scopeIdentifier;
  @NotEmpty final String identifier;
  @NotEmpty final String name;
  @NotNull final Set<String> resourceSelectors;
  @NotNull @Builder.Default final Boolean fullScopeSelected = Boolean.FALSE;
  @NotNull @Builder.Default final Boolean managed = Boolean.FALSE;

  @Setter @CreatedDate Long createdAt;
  @Setter @LastModifiedDate Long lastModifiedAt;
  @Setter @CreatedBy EmbeddedUser createdBy;
  @Setter @LastModifiedBy EmbeddedUser lastUpdatedBy;
  @Setter @Version Long version;

  public boolean isFullScopeSelected() {
    return fullScopeSelected != null && fullScopeSelected;
  }

  public boolean isManaged() {
    return managed != null && managed;
  }

  @Setter Long nextReconciliationIterationAt;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("uniqueResourceGroupPrimaryKey")
                 .field(ResourceGroupDBOKeys.scopeIdentifier)
                 .field(ResourceGroupDBOKeys.identifier)
                 .unique(true)
                 .build())
        .build();
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (ResourceGroupDBOKeys.nextReconciliationIterationAt.equals(fieldName)) {
      nextReconciliationIterationAt = nextIteration;
    }
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextReconciliationIterationAt;
  }

  @Override
  public String getUuid() {
    return id;
  }
}
