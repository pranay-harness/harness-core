package software.wings.service.impl;

import static software.wings.beans.Permit.PERMIT_KEY_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Permit;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.PermitService;

@Singleton
@Slf4j
public class PermitServiceImpl implements PermitService {
  @Inject private WingsPersistence wingsPersistence;
  /*
  {1, 1, 2, 3, 5, 10} == 22 minutes cycle
  500 iterations ~= 80 cycles == 80 * 22 = 1760 > 24hrs
   */
  private static final int[] BACKOFF_MULTIPLIER = new int[] {1, 1, 2, 3, 5, 10};

  public static final int MAX_FAILED_ATTEMPTS = 3500;

  public static int getBackoffMultiplier(int failedCronAttempts) {
    return failedCronAttempts > 500 ? BACKOFF_MULTIPLIER.length - 1
                                    : BACKOFF_MULTIPLIER[failedCronAttempts % BACKOFF_MULTIPLIER.length];
  }

  public static boolean shouldSendAlert(int failedCronAttempts) {
    return MAX_FAILED_ATTEMPTS == failedCronAttempts;
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
