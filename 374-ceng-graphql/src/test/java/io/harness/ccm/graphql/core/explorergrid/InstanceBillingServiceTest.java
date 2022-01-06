/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.graphql.core.explorergrid;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.queryconverter.SQLConverterImpl;
import io.harness.queryconverter.dto.FieldFilter;
import io.harness.queryconverter.dto.GridRequest;
import io.harness.rule.Owner;
import io.harness.timescaledb.Tables;

import org.jooq.impl.TableImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class InstanceBillingServiceTest extends CategoryTest {
  private static final String ACCOUNT_ID = "accountId";

  @Mock SQLConverterImpl converter;
  @InjectMocks InstanceBillingService instanceBillingService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetBillingData() throws Exception {
    ArgumentCaptor<TableImpl> tableCaptor = ArgumentCaptor.forClass(TableImpl.class);
    ArgumentCaptor<GridRequest> requestCaptor = ArgumentCaptor.forClass(GridRequest.class);

    instanceBillingService.getBillingData(ACCOUNT_ID, GridRequest.builder().build());

    verify(converter, times(1)).convert(tableCaptor.capture(), requestCaptor.capture());

    assertThat(requestCaptor.getValue()).isNotNull();

    assertThat(requestCaptor.getValue().getAggregate()).isEmpty();
    assertThat(requestCaptor.getValue().getGroupBy()).isEmpty();
    assertThat(requestCaptor.getValue().getOrderBy()).isEmpty();
    assertThat(requestCaptor.getValue().getHaving()).isEmpty();
    assertThat(requestCaptor.getValue().getLimit()).isNull();
    assertThat(requestCaptor.getValue().getEntity()).isNull();
    assertThat(requestCaptor.getValue().getOffset()).isNull();
    assertThat(requestCaptor.getValue().getWhere()).hasSize(1);

    FieldFilter accountIdFilter = requestCaptor.getValue().getWhere().get(0);
    assertThat(accountIdFilter.getField().toUpperCase()).isEqualTo("ACCOUNTID");
    assertThat(accountIdFilter.getValues()).hasSize(1);
    assertThat(accountIdFilter.getValues().get(0)).isEqualTo(ACCOUNT_ID);

    assertThat(tableCaptor.getValue()).isEqualTo(Tables.BILLING_DATA);
  }
}
