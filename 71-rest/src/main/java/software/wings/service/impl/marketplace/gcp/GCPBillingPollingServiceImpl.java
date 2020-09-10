package software.wings.service.impl.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.marketplace.gcp.GCPBillingJobEntity;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.marketplace.gcp.GCPBillingPollingService;

@OwnedBy(PL)
@Singleton
@Slf4j
public class GCPBillingPollingServiceImpl implements GCPBillingPollingService {
  @Inject private WingsPersistence persistence;

  @Override
  public String create(GCPBillingJobEntity gcpBillingJobEntity) {
    return persistence.save(gcpBillingJobEntity);
  }

  @Override
  public void delete(String accountId) {
    persistence.delete(GCPBillingJobEntity.class, accountId);
  }
}
