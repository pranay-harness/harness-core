/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config;

import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.stereotype.Component;

/**
 * To use Map based JobRepository (In memory)
 */
@Slf4j
@Component
public class NoPersistenceBatchConfigurer extends DefaultBatchConfigurer {
  @Override
  public void setDataSource(DataSource dataSource) {
    log.debug("Using in memory job repository");
  }
}
