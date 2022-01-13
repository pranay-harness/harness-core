/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.budget;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "BudgetAlertsTableKeys")
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class BudgetAlertsTableSchema {
  /**
   * 	TIME TIMESTAMP NOT NULL,
   * 	BUDGETID TEXT NOT NULL,
   * 	ACCOUNTID TEXT NOT NULL,
   *    ALERTTHRESHOLD DOUBLE NOT NULL,
   * 	ACTUALCOST DOUBLE NOT NULL,
   *    BUDGETEDCOST DOUBLE NOT NULL
   */

  DbSpec dbSpec;
  DbSchema dbSchema;
  DbTable budgetAlertsTable;
  DbColumn alertTime;
  DbColumn budgetId;
  DbColumn accountId;
  DbColumn alertThreshold;
  DbColumn alertBasedOn;
  DbColumn actualCost;
  DbColumn budgetedCost;

  private static String doubleType = "double";

  public BudgetAlertsTableSchema() {
    dbSpec = new DbSpec();
    dbSchema = dbSpec.addDefaultSchema();
    budgetAlertsTable = dbSchema.addTable("budget_alerts");
    alertTime = budgetAlertsTable.addColumn("alerttime", "timestamp", null);
    budgetId = budgetAlertsTable.addColumn("budgetid", "text", null);
    accountId = budgetAlertsTable.addColumn("accountid", "text", null);
    alertThreshold = budgetAlertsTable.addColumn("alertthreshold", doubleType, null);
    alertBasedOn = budgetAlertsTable.addColumn("alertbasedon", doubleType, null);
    actualCost = budgetAlertsTable.addColumn("actualcost", doubleType, null);
    budgetedCost = budgetAlertsTable.addColumn("budgetedcost", doubleType, null);
  }
}
