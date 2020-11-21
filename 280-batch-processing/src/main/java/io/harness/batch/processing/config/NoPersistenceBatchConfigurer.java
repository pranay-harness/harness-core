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
