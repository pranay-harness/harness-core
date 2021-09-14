/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.budgets.service.impl;

import static software.wings.graphql.datafetcher.billing.CloudBillingHelper.unified;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.shard.AccountShardService;
import io.harness.ccm.budget.BudgetUtils;
import io.harness.ccm.budget.dao.BudgetDao;
import io.harness.ccm.commons.entities.billing.Budget;

import software.wings.beans.Account;
import software.wings.graphql.datafetcher.billing.CloudBillingHelper;

import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Singleton
@Slf4j
public class BudgetCostUpdateService {
  @Autowired private AccountShardService accountShardService;
  @Autowired private BudgetUtils budgetUtils;
  @Autowired private CloudBillingHelper cloudBillingHelper;
  @Autowired private BatchMainConfig mainConfiguration;
  @Autowired private BudgetDao budgetDao;

  public void updateCosts() {
    List<Account> ceEnabledAccounts = accountShardService.getCeEnabledAccounts();
    List<String> accountIds = ceEnabledAccounts.stream().map(Account::getUuid).collect(Collectors.toList());
    log.info("ceEnabledAccounts ids list {}", accountIds);

    accountIds.forEach(accountId -> {
      String cloudProviderTable = cloudBillingHelper.getCloudProviderTableName(
          mainConfiguration.getBillingDataPipelineConfig().getGcpProjectId(), accountId, unified);
      List<Budget> budgets = budgetUtils.listBudgetsForAccount(accountId);
      budgets.forEach(budget -> {
        budgetUtils.updateBudgetCosts(budget, cloudProviderTable);
        budgetDao.update(budget.getUuid(), budget);
      });
    });
  }
}
