package io.harness.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.retention.AuditRetentionService;
import io.harness.audit.retention.AuditRetentionServiceImpl;

import com.google.inject.AbstractModule;

@OwnedBy(PL)
public class AuditRetentionModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(AuditRetentionService.class).to(AuditRetentionServiceImpl.class);
  }
}
