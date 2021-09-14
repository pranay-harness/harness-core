/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.dao.anomaly;

import io.harness.ccm.commons.entities.anomaly.AnomalyDataList;
import io.harness.ccm.commons.utils.TimescaleUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

@Slf4j
@Singleton
public class AnomalyDao {
  @Inject private DSLContext dslContext;

  public AnomalyDataList getAnomalyData(String query) {
    return TimescaleUtils.retryRun(() -> dslContext.fetchOne(query).into(AnomalyDataList.class));
  }
}
