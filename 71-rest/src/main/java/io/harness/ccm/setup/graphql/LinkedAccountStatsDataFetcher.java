package io.harness.ccm.setup.graphql;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.ce.CECloudAccount;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.ArrayList;
import java.util.List;

public class LinkedAccountStatsDataFetcher
    extends AbstractConnectionV2DataFetcher<QLCESetupFilter, QLCESetupSortCriteria, QLLinkedAccountData> {
  @Inject CESetupQueryHelper ceSetupQueryHelper;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLLinkedAccountData fetchConnection(List<QLCESetupFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLCESetupSortCriteria> sortCriteria) {
    Query<CECloudAccount> query = populateFilters(wingsPersistence, filters, CECloudAccount.class, true);
    if (!sortCriteria.isEmpty()) {
      Sort sort = ceSetupQueryHelper.getSort(sortCriteria.get(0));
      query.order(sort);
    }

    final List<CECloudAccount> linkedAccountsInfo = query.asList();
    int countOfConnected = 0;
    int countOfNotConnected = 0;
    int countOfNotVerified = 0;

    List<QLLinkedAccount> linkedAccountsList = new ArrayList<>();
    for (CECloudAccount ceCloudAccount : linkedAccountsInfo) {
      switch (ceCloudAccount.getAccountStatus()) {
        case CONNECTED:
          countOfConnected++;
          break;
        case NOT_CONNECTED:
          countOfNotConnected++;
          break;
        case NOT_VERIFIED:
          countOfNotVerified++;
          break;
        default:
          break;
      }
      linkedAccountsList.add(populateLinkedAccounts(ceCloudAccount));
    }

    return QLLinkedAccountData.builder()
        .count(QLAccountCountStats.builder()
                   .countOfConnected(countOfConnected)
                   .countOfNotConnected(countOfNotConnected)
                   .countOfNotVerified(countOfNotVerified)
                   .build())
        .linkedAccounts(linkedAccountsList)
        .build();
  }

  private QLLinkedAccount populateLinkedAccounts(CECloudAccount ceCloudAccount) {
    return QLLinkedAccount.builder()
        .id(ceCloudAccount.getInfraAccountId())
        .arn(ceCloudAccount.getAccountArn())
        .name(ceCloudAccount.getAccountName())
        .accountStatus(ceCloudAccount.getAccountStatus())
        .masterAccountId(ceCloudAccount.getInfraMasterAccountId())
        .build();
  }

  @Override
  protected void populateFilters(List<QLCESetupFilter> filters, Query query) {
    ceSetupQueryHelper.setQuery(filters, query);
  }

  @Override
  protected QLCESetupFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    return null;
  }
}
