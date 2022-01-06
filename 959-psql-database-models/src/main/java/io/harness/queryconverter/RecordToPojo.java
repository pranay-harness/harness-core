/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
