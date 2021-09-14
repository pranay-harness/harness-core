/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queryconverter;

import io.harness.timescaledb.tables.pojos.BillingData;
import io.harness.timescaledb.tables.pojos.CeRecommendations;

import java.io.Serializable;
import lombok.Getter;

public enum RecordToPojo {
  BILLING_DATA(BillingData.class),
  CE_RECOMMENDATIONS(CeRecommendations.class);

  @Getter Class<? extends Serializable> pojoClazz;

  RecordToPojo(Class<? extends Serializable> pojoClazz) {
    this.pojoClazz = pojoClazz;
  }
}
