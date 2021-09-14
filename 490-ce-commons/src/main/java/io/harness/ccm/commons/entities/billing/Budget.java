/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.entities.billing;

import io.harness.annotation.StoreIn;
import io.harness.ccm.budget.AlertThreshold;
import io.harness.ccm.budget.BudgetScope;
import io.harness.ccm.budget.BudgetType;
import io.harness.mongo.index.FdIndex;
import io.harness.ng.DbAliases;
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

@Data
@Builder
@StoreIn(DbAliases.CENG)
@FieldNameConstants(innerTypeName = "BudgetKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "budgets", noClassnameStored = true)
public final class Budget implements PersistentEntity, UuidAware, AccountAccess, CreatedAtAware, UpdatedAtAware {
  @Id String uuid;
  @NotBlank @FdIndex String accountId;
  @NotBlank String name;
  @NotBlank BudgetScope scope; // referred to as "Applies to" in the UI
  @NotBlank BudgetType type;
  @NotBlank Double budgetAmount;
  Double actualCost;
  Double forecastCost;
  Double lastMonthCost;
  AlertThreshold[] alertThresholds;
  String[] emailAddresses;
  String[] userGroupIds; // reference
  boolean notifyOnSlack;
  long createdAt;
  long lastUpdatedAt;
}
