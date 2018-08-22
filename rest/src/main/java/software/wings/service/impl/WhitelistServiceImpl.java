package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.security.access.WhitelistStatus.ACTIVE;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.ErrorCode;
import software.wings.beans.FeatureName;
import software.wings.beans.security.access.GlobalWhitelistConfig;
import software.wings.beans.security.access.Whitelist;
import software.wings.beans.security.access.WhitelistConfig;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.WhitelistService;
import software.wings.utils.CacheHelper;
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
public class WhitelistServiceImpl implements WhitelistService {
  private static final Logger logger = LoggerFactory.getLogger(WhitelistServiceImpl.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private CacheHelper cacheHelper;

  @Override
  public Whitelist save(Whitelist whitelist) {
    validate(whitelist);
    Whitelist savedWhitelist = wingsPersistence.saveAndGet(Whitelist.class, whitelist);
    evictWhitelistConfigCache(whitelist.getAccountId());
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
    Validator.notNullCheck("accountId", accountId);
    req.addFilter("accountId", EQ, accountId);
    return wingsPersistence.query(Whitelist.class, req);
  }

  @Override
  public Whitelist get(String accountId, String whitelistId) {
    PageRequest<Whitelist> req =
        aPageRequest().addFilter("accountId", EQ, accountId).addFilter(ID_KEY, EQ, whitelistId).build();
    return wingsPersistence.get(Whitelist.class, req);
  }

  @Override
  public boolean isValidIPAddress(String accountId, String ipAddress) {
    List<Whitelist> whitelistConfigList = getWhitelistConfig(accountId);
    return isValidIPAddress(ipAddress, whitelistConfigList);
  }

  public List<Whitelist> getWhitelistConfig(String accountId) {
    Cache<String, WhitelistConfig> cache = cacheHelper.getWhitelistConfigCache();
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
    Cache<String, WhitelistConfig> cache = cacheHelper.getWhitelistConfigCache();
    cache.remove(accountId);
  }

  @Override
  public boolean isValidIPAddress(String ipAddress, List<Whitelist> whitelistConfigList) {
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
        try {
          SubnetUtils subnetUtils = new SubnetUtils(condition);
          return subnetUtils.getInfo().isInRange(ipAddress);
        } catch (Exception ex) {
          logger.warn("Exception while checking if the ip {} is in range: {}", ipAddress, condition);
          return false;
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
  public Whitelist update(Whitelist whitelist) {
    validate(whitelist);
    UpdateOperations<Whitelist> operations = wingsPersistence.createUpdateOperations(Whitelist.class);

    setUnset(operations, "description", whitelist.getDescription());
    setUnset(operations, "status", whitelist.getStatus());
    setUnset(operations, "filter", whitelist.getFilter());

    Query<Whitelist> query = wingsPersistence.createQuery(Whitelist.class)
                                 .filter(ID_KEY, whitelist.getUuid())
                                 .filter("accountId", whitelist.getAccountId());
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
  public boolean isEnabled(String accountId) {
    return featureFlagService.isEnabled(FeatureName.WHITELIST, accountId);
  }
}
