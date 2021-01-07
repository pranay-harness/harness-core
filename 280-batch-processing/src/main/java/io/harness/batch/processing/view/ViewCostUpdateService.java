package io.harness.batch.processing.view;

import static software.wings.graphql.datafetcher.billing.CloudBillingHelper.unified;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.shard.AccountShardService;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewState;
import io.harness.ccm.views.service.CEViewService;

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
public class ViewCostUpdateService {
  @Autowired private BatchMainConfig config;
  @Autowired private CEViewService ceViewService;
  @Autowired private BigQueryService bigQueryService;
  @Autowired private AccountShardService accountShardService;
  @Autowired private CloudBillingHelper cloudBillingHelper;

  public void updateTotalCost() {
    List<Account> ceEnabledAccounts = accountShardService.getCeEnabledAccounts();
    List<String> accountIds = ceEnabledAccounts.stream().map(Account::getUuid).collect(Collectors.toList());
    accountIds.forEach(accountId -> {
      List<CEView> views = ceViewService.getViewByState(accountId, ViewState.COMPLETED);
      views.forEach(view -> {
        log.info("Updating view {}", view.getUuid());
        ceViewService.updateTotalCost(view, bigQueryService.get(),
            cloudBillingHelper.getCloudProviderTableName(
                config.getBillingDataPipelineConfig().getGcpProjectId(), accountId, unified));
      });
    });
    log.info("Updated views for all accounts");
  }
}
