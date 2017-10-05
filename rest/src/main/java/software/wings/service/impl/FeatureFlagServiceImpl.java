package software.wings.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureFlag.Type;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;

import java.util.List;
import javax.inject.Inject;

public class FeatureFlagServiceImpl implements FeatureFlagService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public boolean getFlag(Type type, String accountId) {
    if (type == null) {
      // we don't want to throw an exception - we just want to log the error
      logger.error("FeatureFlag type is null or missing!");
      return false;
    }

    FeatureFlag ff = wingsPersistence.createQuery(FeatureFlag.class).field("type").equal(type).get();

    if (ff == null) {
      // we don't want to throw an exception - we just want to log the error
      logger.error("FeatureFlag NOT FOUND for type: " + type.name());
      return false;
    }

    if (ff.isFlag()) {
      return true;
    }

    if (accountId == null || accountId.isEmpty()) {
      // we don't want to throw an exception - we just want to log the error
      logger.error("FeatureFlag accountId is null or missing!");
      return false;
    }

    List<String> whiteListedAccountIds = ff.getWhiteListedAccountIds();

    if (whiteListedAccountIds != null && whiteListedAccountIds.size() > 0) {
      if (whiteListedAccountIds.contains(accountId)) {
        return true;
      }
    }

    return false;
  }
}
