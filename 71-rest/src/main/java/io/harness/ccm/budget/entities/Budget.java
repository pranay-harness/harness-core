package io.harness.ccm.budget.entities;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

@Data
@Builder
@FieldNameConstants(innerTypeName = "BudgetKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "budgets", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class Budget
    implements PersistentEntity, UuidAware, AccountAccess, CreatedAtAware, UpdatedAtAware, PersistentRegularIterable {
  @Id String uuid;
  @NotBlank @Indexed String accountId;
  @NotBlank String name;
  @NotBlank BudgetScope scope; // referred to as "Applies to" in the UI
  @NotBlank BudgetType type;
  @NotBlank Double budgetAmount;
  AlertThreshold[] alertThresholds;
  String userGroupId; // reference
  @SchemaIgnore long createdAt;
  @SchemaIgnore long lastUpdatedAt;

  @Indexed Long alertIteration;

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (BudgetKeys.alertIteration.equals(fieldName)) {
      return this.alertIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    if (BudgetKeys.alertIteration.equals(fieldName)) {
      this.alertIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }
}
