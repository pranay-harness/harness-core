package io.harness.ccm.budget;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Whether the Budget is based on a specified amount or based on previous month's actual spend")
public enum BudgetType { SPECIFIED_AMOUNT, PREVIOUS_MONTH_SPEND, PREVIOUS_PERIOD_SPEND }
