package io.harness.app.datafetcher.delegate;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.app.schema.query.delegate.QLDelegateFilter;
import io.harness.app.schema.type.delegate.QLDelegate;
import io.harness.app.schema.type.delegate.QLDelegate.QLDelegateBuilder;
import io.harness.app.schema.type.delegate.QLDelegateList;
import io.harness.app.schema.type.delegate.QLDelegateList.QLDelegateListBuilder;
import io.harness.app.schema.type.delegate.QLDelegateStatus;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;

import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;

public class DelegateListDataFetcher
    extends AbstractConnectionV2DataFetcher<QLDelegateFilter, QLNoOpSortCriteria, QLDelegateList> {
  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLDelegateList fetchConnection(List<QLDelegateFilter> qlDelegateFilters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<Delegate> delegateQuery = populateFilters(wingsPersistence, qlDelegateFilters, Delegate.class, true)
                                        .order(Sort.descending(DelegateKeys.createdAt));

    QLDelegateListBuilder delegateListBuilder = QLDelegateList.builder();
    delegateListBuilder.pageInfo(utils.populate(pageQueryParameters, delegateQuery, delegate -> {
      QLDelegateBuilder qlBuilder = QLDelegate.builder();
      DelegateController.populateQLDelegate(delegate, qlBuilder);
      delegateListBuilder.node(qlBuilder.build());
    }));
    return delegateListBuilder.build();
  }

  @Override
  protected void populateFilters(List<QLDelegateFilter> qlDelegateFilters, Query delegateQuery) {
    if (isEmpty(qlDelegateFilters)) {
      return;
    }
    qlDelegateFilters.forEach(qlDelegateFilter -> {
      FieldEnd<? extends Query<Delegate>> delegateField;
      if (isEmpty(qlDelegateFilter.getDelegateName())) {
        utils.setStringFilter(delegateQuery.field(DelegateKeys.delegateName), qlDelegateFilter.getDelegateName());
      }
      if (qlDelegateFilter.getDelegateStatus() != null) {
        delegateField = delegateQuery.field(DelegateKeys.status);
        QLDelegateStatus filterDelegateStatus = qlDelegateFilter.getDelegateStatus();
        // utils.setEnumFilter(field,filterDelegateStatus);
      }
      if (qlDelegateFilter.getDelegateType() != null) {
        delegateField = delegateQuery.field(DelegateKeys.delegateType);
        String type = qlDelegateFilter.getDelegateType().getStringValue();
        utils.setStringFilter(delegateField, type);
      }
    });
  }

  @Override
  protected QLDelegateFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    return null;
  }
}
