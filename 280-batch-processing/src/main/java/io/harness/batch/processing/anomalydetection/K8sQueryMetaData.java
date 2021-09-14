/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.anomalydetection;

import software.wings.graphql.datafetcher.billing.BillingDataQueryBuilder;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sQueryMetaData {
  String accountId;
  List<DbColumn> selectColumns;
  List<QLCCMAggregationFunction> aggregationList;
  List<QLBillingDataFilter> filtersList;
  List<QLCCMEntityGroupBy> groupByList;
  List<QLBillingSortCriteria> sortCriteria;

  public String getQuery() {
    BillingDataQueryBuilder queryHelper = new BillingDataQueryBuilder();
    return queryHelper.formADQuery(accountId, filtersList, aggregationList, groupByList, sortCriteria, selectColumns);
  }
}
