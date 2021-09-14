/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.service.impl;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.dao.intfc.BatchJobIntervalDao;
import io.harness.batch.processing.service.intfc.BatchJobIntervalService;
import io.harness.ccm.commons.entities.batch.BatchJobInterval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BatchJobIntervalServiceImpl implements BatchJobIntervalService {
  @Autowired private BatchJobIntervalDao batchJobIntervalDao;

  @Override
  public BatchJobInterval fetchBatchJobInterval(String accountId, BatchJobType batchJobType) {
    return batchJobIntervalDao.fetchBatchJobInterval(accountId, batchJobType.name());
  }
}
