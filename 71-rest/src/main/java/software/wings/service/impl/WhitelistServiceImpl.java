package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.security.access.WhitelistStatus.ACTIVE;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.eraro.ErrorCode;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.app.MainConfiguration;
import software.wings.beans.security.access.GlobalWhitelistConfig;
import software.wings.beans.security.access.Whitelist;
import software.wings.beans.security.access.WhitelistConfig;
import software.wings.dl.WingsPersistence;
import software.wings.features.IpWhitelistingFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.features.api.RestrictedApi;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.WhitelistService;
import software.wings.utils.CacheManager;
import software.wings.utils.Validator;

import java.util.Arrays;
import java.util.List;
import javax.cache.Cache;
import javax.validation.executable.ValidateOnExecution;

/**
 * @author rktummala on 04/06/18
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class WhitelistServiceImpl implements WhitelistService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private CacheManager cacheManager;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject @Named(IpWhitelistingFeature.FEATURE_NAME) private PremiumFeature ipWhitelistingFeature;

  @Override
  @RestrictedApi(IpWhitelistingFeature.class)
  public Whitelist save(Whitelist whitelist) {
    validate(whitelist);
    Whitelist savedWhitelist = wingsPersistence.saveAndGet(Whitelist.class, whitelist);
    evictWhitelistConfigCache(whitelist.getAccountId());
    eventPublishHelper.publishSetupIPWhitelistingEvent(savedWhitelist.getAccountId(), savedWhitelist.getUuid());
    return savedWhitelist;
  }

  private void validate(Whitelist whitelist) {
    Validator.notNullCheck("Whitelist object cannot be null", whitelist);
    Validator.notNullCheck("AccountId cannot be null", whitelist.getAccountId());
    String filterCondition = whitelist.getFilter();
    Validator.notNullCheck("Filter condition cannot be null", filterCondition);
    if (filterCondition.contains("/")) {
      try {
        new SubnetUtils(filterCondition);
      } catch (IllegalArgumentException ex) {
        String msg = "Invalid cidr notation : " + filterCondition;
        logger.warn(msg);
        throw new WingsException(ErrorCode.GENERAL_ERROR, USER).addParam("message", msg);
      }
    } else {
      if (!InetAddressValidator.getInstance().isValid(filterCondition)) {
        String msg = "Invalid ip address : " + filterCondition;
        logger.warn(msg);
        throw new WingsException(ErrorCode.GENERAL_ERROR, USER).addParam("message", msg);
      }
    }
  }

  @Override
  public PageResponse<Whitelist> list(String accountId, PageRequest<Whitelist> req) {
    Validator.notNullCheck(Whitelist.ACCOUNT_ID_KEY, accountId);
    req.addFilter(Whitelist.ACCOUNT_ID_KEY, EQ, accountId);
    return wingsPersistence.query(Whitelist.class, req);
  }

  @Override
  public Whitelist get(String accountId, String whitelistId) {
    return wingsPersistence.createQuery(Whitelist.class)
        .filter(Whitelist.ACCOUNT_ID_KEY, accountId)
        .filter(Whitelist.ID_KEY, whitelistId)
        .get();
  }

  @Override
  public boolean isValidIPAddress(String accountId, String ipAddress) {
    if (!ipWhitelistingFeature.isAvailableForAccount(accountId)) {
      return true;
    }

    List<Whitelist> whitelistConfigList = getWhitelistConfig(accountId);
    return isValidIPAddress(ipAddress, whitelistConfigList);
  }

  public List<Whitelist> getWhitelistConfig(String accountId) {
    Cache<String, WhitelistConfig> cache = cacheManager.getWhitelistConfigCache();
    WhitelistConfig value;

    // Cache should never be null, but just in case
    if (cache == null) {
      value = getWhitelistConfigFromDB(accountId);
      return value.getWhitelistList();
    }

    value = cache.get(accountId);
    if (value == null) {
      value = getWhitelistConfigFromDB(accountId);
      cache.put(accountId, value);
    }

    return value.getWhitelistList();
  }

  private WhitelistConfig getWhitelistConfigFromDB(String accountId) {
    List<Whitelist> whitelistConfigFromDB = getWhitelistsFromDB(accountId);
    return WhitelistConfig.builder().accountId(accountId).whitelistList(whitelistConfigFromDB).build();
  }

  private List<Whitelist> getWhitelistsFromDB(String accountId) {
    PageRequest<Whitelist> pageRequest = aPageRequest().addFilter("status", EQ, ACTIVE).build();
    PageResponse<Whitelist> response = list(accountId, pageRequest);
    return response.getResponse();
  }

  private void evictWhitelistConfigCache(String accountId) {
    Cache<String, WhitelistConfig> cache = cacheManager.getWhitelistConfigCache();
    cache.remove(accountId);
  }

  private boolean isValidIPAddress(String ipAddress, List<Whitelist> whitelistConfigList) {
    if (isEmpty(whitelistConfigList)) {
      return true;
    } else {
      boolean isValidClientAddress = checkWhitelist(whitelistConfigList, ipAddress);
      if (isValidClientAddress) {
        return true;
      } else {
        // Check if the request originated from harness network
        return checkDefaultWhitelist(mainConfiguration.getGlobalWhitelistConfig(), ipAddress);
      }
    }
  }

  private boolean checkWhitelist(List<Whitelist> whitelistConfigList, String ipAddress) {
    if (isEmpty(whitelistConfigList)) {
      return false;
    }
    return whitelistConfigList.stream().anyMatch(whitelist -> {
      String condition = whitelist.getFilter();
      if (condition.contains("/")) {
        // Work around when /32 is mentioned in the CIDR.
        // The SubnetUtils doesn't match the ip if /32 is provided.
        if (condition.endsWith("/32")) {
          String[] segments = condition.split("/32");
          return segments[0].equals(ipAddress);
        } else {
          try {
            SubnetUtils subnetUtils = new SubnetUtils(condition);
            return subnetUtils.getInfo().isInRange(ipAddress);
          } catch (Exception ex) {
            logger.warn("Exception while checking if the ip {} is in range: {}", ipAddress, condition);
            return false;
          }
        }
      } else {
        return ipAddress.matches(condition);
      }
    });
  }

  private boolean checkDefaultWhitelist(GlobalWhitelistConfig globalWhitelistConfig, String ipAddress) {
    if (globalWhitelistConfig == null || isEmpty(globalWhitelistConfig.getFilters())) {
      return false;
    }

    String[] filters = globalWhitelistConfig.getFilters().split(",");

    return Arrays.stream(filters).anyMatch(filter -> {
      if (filter.contains("/")) {
        try {
          SubnetUtils subnetUtils = new SubnetUtils(filter);
          boolean inRange = subnetUtils.getInfo().isInRange(ipAddress);
          if (!inRange) {
            logger.warn("ip {} is not in range: {}", ipAddress, filter);
          }
          return inRange;
        } catch (Exception ex) {
          logger.warn("Exception while checking if the ip {} is in range: {}", ipAddress, filter);
          return false;
        }
      } else {
        boolean matches = ipAddress.equals(filter);
        if (!matches) {
          logger.warn("ip {} does not match configured ip filter: {}", ipAddress, filter);
        }
        return matches;
      }
    });
  }

  @Override
  @RestrictedApi(IpWhitelistingFeature.class)
  public Whitelist update(Whitelist whitelist) {
    validate(whitelist);
    UpdateOperations<Whitelist> operations = wingsPersistence.createUpdateOperations(Whitelist.class);

    setUnset(operations, "description", whitelist.getDescription());
    setUnset(operations, "status", whitelist.getStatus());
    setUnset(operations, "filter", whitelist.getFilter());

    Query<Whitelist> query = wingsPersistence.createQuery(Whitelist.class)
                                 .filter(ID_KEY, whitelist.getUuid())
                                 .filter(Whitelist.ACCOUNT_ID_KEY, whitelist.getAccountId());
    wingsPersistence.update(query, operations);
    evictWhitelistConfigCache(whitelist.getAccountId());
    return get(whitelist.getAccountId(), whitelist.getUuid());
  }

  @Override
  public boolean delete(String accountId, String whitelistId) {
    Whitelist whitelist = get(accountId, whitelistId);
    Validator.notNullCheck("whitelist", whitelist);
    Query<Whitelist> whitelistQuery = wingsPersistence.createQuery(Whitelist.class)
                                          .filter(Whitelist.ACCOUNT_ID_KEY, accountId)
                                          .filter(ID_KEY, whitelistId);
    boolean delete = wingsPersistence.delete(whitelistQuery);
    if (delete) {
      evictWhitelistConfigCache(whitelist.getAccountId());
    }
    return delete;
  }

  @Override
  public boolean deleteAll(String accountId) {
    Query<Whitelist> query = wingsPersistence.createQuery(Whitelist.class).filter(Whitelist.ACCOUNT_ID_KEY, accountId);

    return wingsPersistence.delete(query);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    List<Whitelist> whitelists =
        wingsPersistence.createQuery(Whitelist.class).filter(Whitelist.ACCOUNT_ID_KEY, accountId).asList();
    for (Whitelist whitelist : whitelists) {
      delete(accountId, whitelist.getUuid());
    }
  }
}
