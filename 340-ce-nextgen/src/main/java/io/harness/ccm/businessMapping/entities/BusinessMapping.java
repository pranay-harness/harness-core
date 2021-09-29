package io.harness.ccm.businessMapping.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import java.util.List;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@StoreIn(DbAliases.CENG)
@FieldNameConstants(innerTypeName = "BusinessMappingKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "businessMapping", noClassnameStored = true)
@OwnedBy(CE)
public final class BusinessMapping implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware,
                                              AccountAccess, CreatedByAware, UpdatedByAware {
  @Id String uuid;
  @Size(min = 1, max = 32, message = "Name must be between 1 and 32 characters long") @NotBlank String name;
  String accountId;

  List<CostTarget> costTargets;
  List<SharedCost> sharedCosts;

  long createdAt;
  long lastUpdatedAt;
  private EmbeddedUser createdBy;
  private EmbeddedUser lastUpdatedBy;
}
