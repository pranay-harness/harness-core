/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.budget;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLFilterKind;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class BudgetAlertsQueryMetadata {
  private static final BudgetAlertsTableSchema schema = new BudgetAlertsTableSchema();
  enum DataType { STRING, INTEGER, TIMESTAMP, DOUBLE }

  public enum BudgetAlertsMetaDataFields {
    ALERTTIME("ALERTTIME", DataType.TIMESTAMP, QLFilterKind.SIMPLE),
    BUDGETID("BUDGETID", DataType.STRING, QLFilterKind.SIMPLE),
    ACCOUNTID("ACCOUNTID", DataType.STRING, QLFilterKind.SIMPLE),
    ACTUALCOST("ACTUALCOST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    BUDGETEDCOST("BUDGETEDCOST", DataType.DOUBLE, QLFilterKind.SIMPLE),
    ALERTTHRESHOLD("ALERTTHRESHOLD", DataType.DOUBLE, QLFilterKind.SIMPLE);
    private DataType dataType;
    private String fieldName;
    private QLFilterKind filterKind;

    BudgetAlertsMetaDataFields(String fieldName, DataType dataType, QLFilterKind filterKind) {
      this.fieldName = fieldName;
      this.dataType = dataType;
      this.filterKind = filterKind;
    }

    public QLFilterKind getFilterKind() {
      return filterKind;
    }

    public DataType getDataType() {
      return dataType;
    }

    public String getFieldName() {
      return fieldName;
    }
  }

  List<BudgetAlertsQueryMetadata> groupByFields;

  private List<BudgetAlertsMetaDataFields> fieldNames;

  private List<QLBillingSortCriteria> sortCriteria;

  private String query;

  List<QLBillingDataFilter> filters;
}
