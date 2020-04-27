package io.harness.ccm.setup.graphql;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CECloudAccount.CECloudAccountKeys;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import java.util.List;

@Singleton
public class CESetupQueryHelper {
  @Inject protected DataFetcherUtils utils;

  public void setQuery(List<QLCESetupFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<SettingAttribute>> field;

      if (filter.getCloudProviderId() != null) {
        field = query.field("cloudProviderId");
        QLIdFilter cloudProviderIdFilter = filter.getCloudProviderId();
        utils.setIdFilter(field, cloudProviderIdFilter);
      }

      if (filter.getInfraMasterAccountId() != null) {
        field = query.field("infraMasterAccountId");
        QLIdFilter infraMasterAccountIdFilter = filter.getInfraMasterAccountId();
        utils.setIdFilter(field, infraMasterAccountIdFilter);
      }

      if (filter.getMasterAccountSettingId() != null) {
        field = query.field("masterAccountSettingId");
        QLIdFilter masterAccountSettingIdFilter = filter.getMasterAccountSettingId();
        utils.setIdFilter(field, masterAccountSettingIdFilter);
      }

      if (filter.getSettingId() != null) {
        field = query.field("_id");
        QLIdFilter settingIdFilter = filter.getSettingId();
        utils.setIdFilter(field, settingIdFilter);
      }
    });
  }

  public Sort getSort(QLCESetupSortCriteria sort) {
    String sortField = null;
    if (sort.getSortType() == QLCESetupSortType.status) {
      sortField = CECloudAccountKeys.accountStatus;
    }
    switch (sort.getSortOrder()) {
      case DESCENDING:
        return Sort.descending(sortField);
      case ASCENDING:
      default:
        return Sort.ascending(sortField);
    }
  }
}
