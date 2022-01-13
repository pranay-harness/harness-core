/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
