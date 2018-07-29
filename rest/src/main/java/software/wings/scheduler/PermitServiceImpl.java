package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;

/**
 * Created by anubhaw on 7/17/18.
 */
@Singleton
public class PermitServiceImpl implements PermitService {
  @Inject private WingsPersistence wingsPersistence;
  private static final Logger logger = LoggerFactory.getLogger(PermitService.class);
  private static final int[] BACKOFF_MULTIPLIER = new int[] {1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610};

  public static int getBackoffMultiplier(int failedCronAttempts) {
    return BACKOFF_MULTIPLIER[failedCronAttempts % BACKOFF_MULTIPLIER.length];
  }

  @Override
  public String acquirePermit(Permit permit) {
    try {
      return wingsPersistence.save(permit);
    } catch (DuplicateKeyException ex) {
      logger.info("Permit [{}] already exists", permit);
    } catch (Exception ex) {
      logger.error("Unexpected error in issuing permit", ex);
    }
    return null;
  }

  @Override
  public boolean releasePermit(String permitId) {
    Query<Permit> query = wingsPersistence.createQuery(Permit.class).field(Mapper.ID_KEY).equal(permitId);
    Permit permit = query.get();
    if (permit == null) {
      logger.info("Permit [{}] already deleted", permitId);
      return true;
    }
    return wingsPersistence.delete(query);
  }

  //  @Override
  //  public boolean releasePermit(String permitId, boolean withFailure) {
  //    Query<Permit> query = wingsPersistence.createQuery(Permit.class).field(Mapper.ID_KEY).equal(permitId);
  //    Permit permit = query.get();
  //    if (permit == null) {
  //      logger.info("Permit [{}] already deleted", permitId);
  //      return true;
  //    }
  //
  //    if (withFailure) {
  //      int failedAttempt = (permit.getFailedAttempt() + 1) % BACKOFF_MULTIPLIER.length;
  //      long updatedExpiry = permit.getExpireAt().getTime() + permit.getLeaseDuration() * failedAttempt;
  //      Date expireAt = new Date(updatedExpiry); // TODO:: extend same date object
  //      UpdateResults update = wingsPersistence.update(permit,
  //          wingsPersistence.createUpdateOperations(Permit.class)
  //              .set("failedAttempt", failedAttempt)
  //              .set("expireAt", expireAt));
  //      if (update.getUpdatedExisting()) {
  //        logger.info("Permit updated with new expiry [{}] on [{}] failed attempt", updatedExpiry, failedAttempt);
  //      } else {
  //        logger.warn("Expiry update failed for Permit [{}]", permit);
  //      }
  //    } else {
  //      return wingsPersistence.delete(query);
  //    }
  //    return true;
  //  }
}
