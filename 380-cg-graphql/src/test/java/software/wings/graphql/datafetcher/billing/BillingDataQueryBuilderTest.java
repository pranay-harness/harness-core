/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;

import com.healthmarketscience.sqlbuilder.Converter;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class BillingDataQueryBuilderTest extends AbstractDataFetcherTestBase {
  private final BillingDataTableSchema schema = new BillingDataTableSchema();

  @InjectMocks BillingDataQueryBuilder billingDataQueryBuilder;

  private SelectQuery selectQuery;
  private List<BillingDataMetaDataFields> fieldNames;

  @Before
  public void setup() {
    selectQuery = new SelectQuery();
    fieldNames = new ArrayList<>();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testDecorateSUMQueryWithAggregation() {
    String graphqlColumnName = "storageCost";
    BillingDataMetaDataFields expectedField = BillingDataMetaDataFields.STORAGECOST;

    SelectQuery expectedSelectQuery = new SelectQuery().addCustomColumns(Converter.toColumnSqlObject(
        FunctionCall.sum().addColumnParams(schema.getStorageCost()), expectedField.getFieldName()));

    billingDataQueryBuilder.decorateQueryWithAggregation(
        selectQuery, makeAggregationFunction(graphqlColumnName, QLCCMAggregateOperation.SUM), fieldNames);

    assertThat(selectQuery.toString()).isEqualTo(expectedSelectQuery.toString());
    assertThat(fieldNames).contains(expectedField);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testDecorateMAXQueryWithAggregation() {
    String graphqlColumnName = "storageRequest";
    BillingDataMetaDataFields expectedField = BillingDataMetaDataFields.STORAGEREQUEST;

    SelectQuery expectedSelectQuery = new SelectQuery().addCustomColumns(Converter.toColumnSqlObject(
        FunctionCall.max().addColumnParams(schema.getStorageRequest()), expectedField.getFieldName()));

    billingDataQueryBuilder.decorateQueryWithAggregation(
        selectQuery, makeAggregationFunction(graphqlColumnName, QLCCMAggregateOperation.MAX), fieldNames);

    assertThat(selectQuery.toString()).isEqualTo(expectedSelectQuery.toString());
    assertThat(fieldNames).contains(expectedField);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testDecorateAVGQueryWithAggregation() {
    String graphqlColumnName = "storageRequest";
    BillingDataMetaDataFields expectedField = BillingDataMetaDataFields.STORAGEREQUEST;

    SelectQuery expectedSelectQuery = new SelectQuery().addCustomColumns(Converter.toColumnSqlObject(
        FunctionCall.avg().addColumnParams(schema.getStorageRequest()), expectedField.getFieldName()));

    billingDataQueryBuilder.decorateQueryWithAggregation(
        selectQuery, makeAggregationFunction(graphqlColumnName, QLCCMAggregateOperation.AVG), fieldNames);

    assertThat(selectQuery.toString()).isEqualTo(expectedSelectQuery.toString());
    assertThat(fieldNames).contains(expectedField);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testDecorateQueryWithAggregationForUnknownColumn() {
    String graphqlColumnName = "kdsjbvkjds";

    billingDataQueryBuilder.decorateQueryWithAggregation(
        new SelectQuery(), makeAggregationFunction(graphqlColumnName, QLCCMAggregateOperation.MAX), new ArrayList<>());
  }

  private QLCCMAggregationFunction makeAggregationFunction(String graphqlColumnName, QLCCMAggregateOperation ops) {
    return QLCCMAggregationFunction.builder().columnName(graphqlColumnName).operationType(ops).build();
  }
}
