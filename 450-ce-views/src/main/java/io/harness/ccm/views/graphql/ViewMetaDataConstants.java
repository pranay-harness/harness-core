/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.views.graphql;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ViewMetaDataConstants {
  public static final String entityConstantMinStartTime = "startTime_MIN";
  public static final String entityConstantMaxStartTime = "startTime_MAX";
  public static final String entityConstantCost = "cost";
  public static final String entityConstantClusterCost = "billingamount";
  public static final String entityConstantIdleCost = "actualidlecost";
  public static final String entityConstantUnallocatedCost = "unallocatedcost";
  public static final String entityConstantSystemCost = "systemcost";
}
