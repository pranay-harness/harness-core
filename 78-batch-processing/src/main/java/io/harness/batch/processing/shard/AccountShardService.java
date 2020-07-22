package io.harness.batch.processing.shard;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.config.PodInfoConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.wings.beans.Account;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AccountShardService {
  private BatchMainConfig mainConfig;
  private CloudToHarnessMappingService cloudToHarnessMappingService;

  @Autowired
  public AccountShardService(BatchMainConfig mainConfig, CloudToHarnessMappingService cloudToHarnessMappingService) {
    this.mainConfig = mainConfig;
    this.cloudToHarnessMappingService = cloudToHarnessMappingService;
  }

  public List<Account> getCeEnabledAccounts() {
    logger.info("Shard Id {} master pod {}", getShardId(), isMasterPod());
    int shardId = getShardId();
    PodInfoConfig podInfoConfig = mainConfig.getPodInfoConfig();
    int replicas = podInfoConfig.getReplica();
    List<Account> ceEnabledAccounts = cloudToHarnessMappingService.getCeEnabledAccounts();
    List<Account> accounts = ceEnabledAccounts.stream()
                                 .filter(account -> eligibleAccount(account.getUuid(), shardId, replicas))
                                 .collect(Collectors.toList());
    List<String> collect = accounts.stream().map(Account::getUuid).collect(Collectors.toList());
    logger.info("Account size {} :: {} :: {}", ceEnabledAccounts.size(), accounts.size(), collect);
    return accounts;
  }

  private int getShardId() {
    PodInfoConfig podInfoConfig = mainConfig.getPodInfoConfig();
    String podName = podInfoConfig.getName();
    return Integer.parseInt(podName.substring(podName.lastIndexOf('-') + 1));
  }

  private boolean eligibleAccount(String accountId, int shardId, int replicas) {
    int hash = accountId.hashCode();
    int index = Math.abs(hash % replicas);
    return index == shardId;
  }

  public boolean isMasterPod() {
    int shardId = getShardId();
    return shardId == 0;
  }
}
