package software.wings.service.impl.marketplace.gcp;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.marketplace.gcp.GCPBillingJobEntity;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.marketplace.gcp.GCPBillingPollingService;

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
