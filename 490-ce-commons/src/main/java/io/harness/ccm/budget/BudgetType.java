/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.budget;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Whether the Budget is based on a specified amount or based on previous month's actual spend")
public enum BudgetType {
  SPECIFIED_AMOUNT,
  PREVIOUS_MONTH_SPEND,
  PREVIOUS_PERIOD_SPEND
}
