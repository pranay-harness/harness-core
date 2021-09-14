/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.billing.reader;

import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.entities.batch.InstanceData;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InstanceDataReader {
  private String accountId;
  private List<InstanceType> instanceTypes;
  private Instant activeInstanceIterator;
  private Instant endTime;
  private int batchSize;
  private InstanceDataDao instanceDataDao;

  public InstanceDataReader(InstanceDataDao instanceDataDao, String accountId, List<InstanceType> instanceTypes,
      Instant activeInstanceIterator, Instant endTime, int batchSize) {
    this.accountId = accountId;
    this.instanceTypes = instanceTypes;
    this.activeInstanceIterator = activeInstanceIterator;
    this.endTime = endTime;
    this.batchSize = batchSize;
    this.instanceDataDao = instanceDataDao;
  }

  public List<InstanceData> getNext() {
    List<InstanceData> instanceDataLists = instanceDataDao.getInstanceDataListsOfTypes(
        accountId, batchSize, activeInstanceIterator, endTime, instanceTypes);
    if (!instanceDataLists.isEmpty()) {
      activeInstanceIterator = instanceDataLists.get(instanceDataLists.size() - 1).getActiveInstanceIterator();
      if (instanceDataLists.get(0).getActiveInstanceIterator().equals(activeInstanceIterator)) {
        log.info("Incrementing lastActiveInstanceIterator by 1ms {} {} {} {}", instanceDataLists.size(),
            activeInstanceIterator, endTime, accountId);
        activeInstanceIterator = activeInstanceIterator.plus(1, ChronoUnit.MILLIS);
      }
    }
    return instanceDataLists;
  }
}
