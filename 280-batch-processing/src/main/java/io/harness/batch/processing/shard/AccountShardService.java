/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.shard;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.config.PodInfoConfig;
import io.harness.batch.processing.dao.intfc.AccountShardMappingDao;
import io.harness.batch.processing.entities.AccountShardMapping;

import software.wings.beans.Account;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountShardService {
  private BatchMainConfig mainConfig;
  private CloudToHarnessMappingService cloudToHarnessMappingService;
  private AccountShardMappingDao accountShardMappingDao;

  @Autowired
  public AccountShardService(BatchMainConfig mainConfig, CloudToHarnessMappingService cloudToHarnessMappingService,
      AccountShardMappingDao accountShardMappingDao) {
    this.mainConfig = mainConfig;
    this.cloudToHarnessMappingService = cloudToHarnessMappingService;
    this.accountShardMappingDao = accountShardMappingDao;
  }

  public List<Account> getCeEnabledAccounts() {
    log.info("Shard Id {} master pod {}", getShardId(), isMasterPod());
    List<AccountShardMapping> accountShardMappings = accountShardMappingDao.getAccountShardMapping();
    List<String> isolatedAccounts =
        accountShardMappings.stream().map(AccountShardMapping::getAccountId).collect(Collectors.toList());
    int shardId = getShardId();
    int replicas = getReplicaCount();
    List<Account> ceEnabledAccounts = cloudToHarnessMappingService.getCeEnabledAccounts();
    List<Account> accounts;

    if (checkIsolatedPod(shardId, replicas)) {
      List<String> eligibleIsolatedAccounts =
          accountShardMappings.stream()
              .filter(accountShardMapping -> eligibleIsolatedAccount(accountShardMapping, shardId, replicas))
              .map(AccountShardMapping::getAccountId)
              .collect(Collectors.toList());
      accounts = ceEnabledAccounts.stream()
                     .filter(account -> eligibleIsolatedAccounts.contains(account.getUuid()))
                     .collect(Collectors.toList());
    } else {
      accounts = ceEnabledAccounts.stream()
                     .filter(account -> !isolatedAccounts.contains(account.getUuid()))
                     .filter(account -> eligibleAccount(account.getUuid(), shardId, replicas))
                     .collect(Collectors.toList());
    }
    List<String> collect = accounts.stream().map(Account::getUuid).collect(Collectors.toList());
    log.info("Account size {} :: {} :: {}", ceEnabledAccounts.size(), accounts.size(), collect);
    return accounts;
  }

  private int getShardId() {
    PodInfoConfig podInfoConfig = mainConfig.getPodInfoConfig();
    String podName = podInfoConfig.getName();
    return Integer.parseInt(podName.substring(podName.lastIndexOf('-') + 1));
  }

  private boolean checkIsolatedPod(int shardId, int replicaCount) {
    return shardId >= replicaCount;
  }

  private int getReplicaCount() {
    PodInfoConfig podInfoConfig = mainConfig.getPodInfoConfig();
    int replica = podInfoConfig.getReplica();
    int isolatedReplica = podInfoConfig.getIsolatedReplica();
    return replica - isolatedReplica;
  }

  private boolean eligibleAccount(String accountId, int shardId, int replicas) {
    int hash = accountId.hashCode();
    int index = Math.abs(hash % replicas);
    return index == shardId;
  }

  private boolean eligibleIsolatedAccount(AccountShardMapping accountShardMapping, int shardId, int replicas) {
    return shardId + 1 == accountShardMapping.getShardId() + replicas;
  }

  public boolean isMasterPod() {
    int shardId = getShardId();
    return shardId == 0;
  }
}
