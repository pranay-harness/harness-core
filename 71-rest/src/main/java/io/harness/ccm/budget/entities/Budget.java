package io.harness.ccm.budget.entities;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

@Data
@FieldNameConstants(innerTypeName = "ClusterRecordKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "budgets", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class Budget implements PersistentEntity, UuidAware, AccountAccess, CreatedAtAware {
  @Id String uuid;
  @Indexed String accountId;
  String name;
  BudgetScope scope; // referred to as "Applies to" in the UI
  BudgetType type;
  Double budgetAmount;
  AlertThreshold[] alertThresholds;
  @SchemaIgnore long createdAt;
}
