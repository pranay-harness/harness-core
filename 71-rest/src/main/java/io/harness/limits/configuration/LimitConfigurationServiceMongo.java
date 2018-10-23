package io.harness.limits.configuration;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.limits.lib.Limit;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.UpdateOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.dl.WingsPersistence;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@Singleton
@ParametersAreNonnullByDefault
public class LimitConfigurationServiceMongo implements LimitConfigurationService {
  private static final Logger log = LoggerFactory.getLogger(LimitConfigurationServiceMongo.class);

  @Inject private WingsPersistence dao;

  @Override
  public @Nullable ConfiguredLimit getOrDefault(String accountId, ActionType actionType) {
    ConfiguredLimit limit = get(accountId, actionType);

    if (null == limit) {
      limit = getDefaultLimit(actionType);
    }

    return limit;
  }

  @VisibleForTesting
  @Nullable
  ConfiguredLimit get(String accountId, ActionType actionType) {
    return dao.createQuery(ConfiguredLimit.class).filter("accountId", accountId).filter("key", actionType).get();
  }

  private @Nullable ConfiguredLimit getDefaultLimit(ActionType actionType) {
    return dao.createQuery(ConfiguredLimit.class)
        .filter("accountId", Base.GLOBAL_ACCOUNT_ID)
        .filter("key", actionType)
        .get();
  }

  @Override
  public boolean configure(String accountId, ActionType actionType, Limit limit) {
    ConfiguredLimit configuredLimit;

    switch (limit.getLimitType()) {
      case STATIC:
        configuredLimit = new ConfiguredLimit<>(accountId, (StaticLimit) limit, actionType);
        break;
      case RATE_LIMIT:
        configuredLimit = new ConfiguredLimit<>(accountId, (RateLimit) limit, actionType);
        break;
      default:
        throw new IllegalArgumentException("Unknown limit type: " + limit.getLimitType());
    }

    Datastore ds = dao.getDatastore();

    UpdateOperations<ConfiguredLimit> updateOp = ds.createUpdateOperations(ConfiguredLimit.class)
                                                     .set("accountId", configuredLimit.getAccountId())
                                                     .set("key", configuredLimit.getKey())
                                                     .set("limit", configuredLimit.getLimit());

    Query<ConfiguredLimit> query = ds.createQuery(ConfiguredLimit.class)
                                       .field("accountId")
                                       .equal(configuredLimit.getAccountId())
                                       .field("key")
                                       .equal(configuredLimit.getKey());

    UpdateOptions options = new UpdateOptions();
    options.upsert(true);

    UpdateResults results = ds.update(query, updateOp, options);
    if (results.getUpdatedCount() < 1 && results.getInsertedCount() < 1) {
      log.error(
          "Both inserted and updated count are less than 1. UpdateCount: {}, InsertCount: {}, Account ID: {}, actionType: {}, Limit: {}",
          results.getUpdatedCount(), results.getInsertedCount(), accountId, actionType, limit);
      return false;
    }

    return true;
  }
}