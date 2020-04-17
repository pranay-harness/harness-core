package io.harness.ccm.setup.graphql;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import org.mongodb.morphia.query.Query;
import software.wings.beans.ce.CECloudAccount;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.ArrayList;
import java.util.List;

public class LinkedAccountStatsDataFetcher
    extends AbstractConnectionV2DataFetcher<QLCESetupFilter, QLNoOpSortCriteria, QLLinkedAccountData> {
  @Inject CESetupQueryHelper ceSetupQueryHelper;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLLinkedAccountData fetchConnection(
      List<QLCESetupFilter> filters, QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<CECloudAccount> query = populateFilters(wingsPersistence, filters, CECloudAccount.class, true);
    final List<CECloudAccount> linkedAccountsInfo = query.asList();

    int numberOfClusters = linkedAccountsInfo.size();
    List<QLLinkedAccount> linkedAccountsList = new ArrayList<>();
    for (CECloudAccount ceCloudAccount : linkedAccountsInfo) {
      linkedAccountsList.add(populateLinkedAccounts(ceCloudAccount));
    }

    return QLLinkedAccountData.builder().count(numberOfClusters).linkedAccounts(linkedAccountsList).build();
  }

  private QLLinkedAccount populateLinkedAccounts(CECloudAccount ceCloudAccount) {
    return QLLinkedAccount.builder()
        .id(ceCloudAccount.getInfraAccountId())
        .arn(ceCloudAccount.getAccountArn())
        .name(ceCloudAccount.getAccountName())
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
