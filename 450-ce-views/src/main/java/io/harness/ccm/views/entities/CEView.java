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
import javax.validation.constraints.Size;

@Data
@Builder
@StoreIn("events")
@FieldNameConstants(innerTypeName = "CEViewKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceView", noClassnameStored = true)
public class CEView implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess,
                               CreatedByAware, UpdatedByAware {
  @Id String uuid;
  @Size(min = 1, max = 32, message = "for view must be between 1 and 32 characters long") @NotBlank String name;
  String accountId;
  @NotBlank String viewVersion;

  ViewTimeRange viewTimeRange;
  List<ViewRule> viewRules;
  ViewVisualization viewVisualization;
  ViewType viewType = ViewType.CUSTOMER;

  ViewState viewState = ViewState.DRAFT;
  long createdAt;
  long lastUpdatedAt;
  private EmbeddedUser createdBy;
  private EmbeddedUser lastUpdatedBy;
}
