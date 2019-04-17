package software.wings.service.impl;

import static software.wings.beans.Permit.PERMIT_KEY_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Permit;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.PermitService;

@Singleton
public class PermitServiceImpl implements PermitService {
  @Inject private WingsPersistence wingsPersistence;
  private static final Logger logger = LoggerFactory.getLogger(PermitService.class);
  private static final int[] BACKOFF_MULTIPLIER = new int[] {1, 1, 2, 3, 5, 8, 13, 21, 34, 34, 34, 34};

  public static final int MAX_FAILED_ATTEMPTS = 500;

  public static int getBackoffMultiplier(int failedCronAttempts) {
    return BACKOFF_MULTIPLIER[failedCronAttempts % BACKOFF_MULTIPLIER.length];
  }

  public static boolean shouldSendAlert(int failedCronAttempts) {
    return failedCronAttempts == 7;
  }

  @Override
  public String acquirePermit(Permit permit) {
    try {
      return wingsPersistence.save(permit);
    } catch (DuplicateKeyException ex) {
      logger.info("Permit already exists for key[{}] in group [{}]", permit.getKey(), permit.getGroup());
    } catch (Exception ex) {
      logger.error("Unexpected error in issuing permit", ex);
    }
    return null;
  }

  @Override
  public boolean releasePermitByKey(String key) {
    Permit permit = wingsPersistence.createQuery(Permit.class).filter(PERMIT_KEY_ID, key).get();
    if (permit == null) {
      logger.info("Permit with key [{}] already deleted", key);
      return true;
    }
    return wingsPersistence.delete(permit);
  }
}
