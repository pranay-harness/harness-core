package software.wings.scheduler.marketplace.gcp;

import static java.time.Duration.ofMinutes;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.marketplace.gcp.GCPBillingJobEntity;
import software.wings.beans.marketplace.gcp.GCPBillingJobEntity.GCPBillingJobEntityKeys;
import software.wings.service.intfc.marketplace.gcp.GCPMarketPlaceService;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GCPBillingHandler implements Handler<GCPBillingJobEntity> {
  @Inject GCPMarketPlaceService gcpMarketPlaceService;

  public static class GCPBillingExecutor {
    static int POOL_SIZE = 5;
    public static void registerIterators(Injector injector) {
      final ScheduledThreadPoolExecutor executor =
          new ScheduledThreadPoolExecutor(POOL_SIZE, new ThreadFactoryBuilder().setNameFormat("GCPBilling").build());

      final GCPBillingHandler handler = new GCPBillingHandler();
      injector.injectMembers(handler);

      Semaphore semaphore = new Semaphore(POOL_SIZE);
      PersistenceIterator iterator = MongoPersistenceIterator.<GCPBillingJobEntity>builder()
                                         .clazz(GCPBillingJobEntity.class)
                                         .fieldName(GCPBillingJobEntityKeys.nextIteration)
                                         .targetInterval(ofMinutes(1440))
                                         .acceptableDelay(ofMinutes(1))
                                         .executorService(executor)
                                         .semaphore(semaphore)
                                         .handler(handler)
                                         .redistribute(true)
                                         .build();

      injector.injectMembers(iterator);
      executor.scheduleAtFixedRate(() -> iterator.process(ProcessMode.PUMP), 0, 60, TimeUnit.MINUTES);
    }
  }

  @Override
  public void handle(GCPBillingJobEntity entity) {
    logger.info("Inside GCP billing handler !! {} ", entity.toString());
    try {
      gcpMarketPlaceService.createUsageReport(entity.getAccountId(), entity.getGcpAccountId());
    } catch (Exception ex) {
      logger.error("GCP_MKT_PLACE exception in handling request : ", ex);
    }
  }
}
