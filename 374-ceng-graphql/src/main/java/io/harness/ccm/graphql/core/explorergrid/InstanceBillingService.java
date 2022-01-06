/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.graphql.core.explorergrid;

import static io.harness.timescaledb.Tables.BILLING_DATA;

import static java.util.Collections.singletonList;

import io.harness.queryconverter.SQLConverter;
import io.harness.queryconverter.dto.FieldFilter;
import io.harness.queryconverter.dto.FilterOperator;
import io.harness.queryconverter.dto.GridRequest;
import io.harness.timescaledb.tables.pojos.BillingData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;

@Singleton
public class InstanceBillingService {
  @Inject SQLConverter converter;

  @SneakyThrows
  public List<BillingData> getBillingData(String accountId, GridRequest request) {
    List<FieldFilter> filters = new ArrayList<>();

    filters.add(createAccountIdFilter(accountId));
    filters.addAll(request.getWhere());

    request.setWhere(filters);

    return (List<BillingData>) converter.convert(BILLING_DATA, request);
  }

  private static FieldFilter createAccountIdFilter(String value) {
    return FieldFilter.builder()
        .field(BILLING_DATA.ACCOUNTID.getName())
        .operator(FilterOperator.EQUALS)
        .values(singletonList(value))
        .build();
  }
}
