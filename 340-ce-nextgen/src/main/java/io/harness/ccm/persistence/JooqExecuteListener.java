package io.harness.ccm.persistence;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;

@Slf4j
@Singleton
public class JooqExecuteListener extends DefaultExecuteListener {
  @Override
  public void executeStart(ExecuteContext ctx) {
    log.info("PSQL Query:\n{}", ctx.query());
  }
}
