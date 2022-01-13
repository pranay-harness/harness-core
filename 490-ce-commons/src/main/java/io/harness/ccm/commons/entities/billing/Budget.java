/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.entities.billing;

import io.harness.annotation.StoreIn;
import io.harness.ccm.budget.AlertThreshold;
import io.harness.ccm.budget.BudgetPeriod;
import io.harness.ccm.budget.BudgetScope;
import io.harness.ccm.budget.BudgetType;
import io.harness.mongo.index.FdIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "Budget", description = "The Cloud Cost Budget definition")
public final class Budget implements PersistentEntity, UuidAware, AccountAccess, CreatedAtAware, UpdatedAtAware {
  @Id String uuid;
  @NotBlank @FdIndex String accountId;
  @NotBlank String name;
  @NotBlank BudgetScope scope; // referred to as "Applies to" in the UI
  @NotBlank BudgetType type;
  @NotBlank Double budgetAmount;
  @NotBlank BudgetPeriod period;
  Double growthRate;
  Double actualCost;
  Double forecastCost;
  Double lastMonthCost;
  AlertThreshold[] alertThresholds;
  String[] emailAddresses;
  String[] userGroupIds; // reference
  boolean notifyOnSlack;
  boolean isNgBudget;
  long startTime;
  long endTime;
  long createdAt;
  long lastUpdatedAt;
}
