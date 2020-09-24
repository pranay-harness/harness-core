package io.harness.ccm.views.entities;

import io.harness.annotation.StoreIn;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.List;

@Data
@Builder
@StoreIn("events")
@FieldNameConstants(innerTypeName = "CEViewKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceView", noClassnameStored = true)
public class CEView implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess,
                               CreatedByAware, UpdatedByAware {
  @Id String uuid;

  @NotBlank String name;
  @NotBlank String accountId;
  @NotBlank String viewVersion;

  ViewTimeRange viewTimeRange;
  List<ViewRule> viewRules;
  ViewVisualization viewVisualization;
  ViewType viewType;

  long createdAt;
  long lastUpdatedAt;
  private EmbeddedUser createdBy;
  private EmbeddedUser lastUpdatedBy;
}
