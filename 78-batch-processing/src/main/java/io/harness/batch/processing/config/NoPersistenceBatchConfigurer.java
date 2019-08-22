package io.harness.batch.processing.config;

import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * To use Map based JobRepository (In memory)
 */
@Component
public class NoPersistenceBatchConfigurer extends DefaultBatchConfigurer {
  @Override
  public void setDataSource(DataSource dataSource) {}
}
