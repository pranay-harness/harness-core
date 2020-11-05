package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.validation.Validator.notNullCheck;
import static org.apache.commons.validator.routines.InetAddressValidator.getInstance;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.app.ManagerCacheRegistrar.WHITELIST_CACHE;
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
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Event.Type;
import software.wings.beans.security.access.Whitelist;
import software.wings.beans.security.access.WhitelistConfig;
import software.wings.dl.WingsPersistence;
import software.wings.features.IpWhitelistingFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.features.api.RestrictedApi;
import software.wings.service.intfc.WhitelistService;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
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
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject @Named(WHITELIST_CACHE) private Cache<String, WhitelistConfig> whitelistConfigCache;
  @Inject @Named(IpWhitelistingFeature.FEATURE_NAME) private PremiumFeature ipWhitelistingFeature;

  @Override
  @RestrictedApi(IpWhitelistingFeature.class)
  public Whitelist save(Whitelist whitelist) {
    validate(whitelist);
    Whitelist savedWhitelist = wingsPersistence.saveAndGet(Whitelist.class, whitelist);
    evictWhitelistConfigCache(whitelist.getAccountId());
    eventPublishHelper.publishSetupIPWhitelistingEvent(savedWhitelist.getAccountId(), savedWhitelist.getUuid());
    log.info("Created whitelist config {} for account {}", savedWhitelist.getUuid(), savedWhitelist.getAccountId());
    auditServiceHelper.reportForAuditingUsingAccountId(whitelist.getAccountId(), null, whitelist, Type.CREATE);
    return savedWhitelist;
  }

  private void validate(Whitelist whitelist) {
    notNullCheck("Whitelist object cannot be null", whitelist);
    notNullCheck("AccountId cannot be null", whitelist.getAccountId());
    String filterCondition = whitelist.getFilter();
    notNullCheck("Filter condition cannot be null", filterCondition);
    if (filterCondition.contains("/")) {
      try {
        new SubnetUtils(filterCondition);
      } catch (IllegalArgumentException ex) {
        String msg = "Invalid cidr notation : " + filterCondition;
        log.warn(msg);
        throw new WingsException(ErrorCode.GENERAL_ERROR, USER).addParam("message", msg);
      }
    } else {
      if (!getInstance().isValid(filterCondition)) {
        String msg = "Invalid ip address : " + filterCondition;
        log.warn(msg);
        throw new WingsException(ErrorCode.GENERAL_ERROR, USER).addParam("message", msg);
      }
    }
  }

  @Override
  public PageResponse<Whitelist> list(String accountId, PageRequest<Whitelist> req) {
    notNullCheck(Whitelist.ACCOUNT_ID_KEY, accountId);
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
    return checkWhitelist(whitelistConfigList, ipAddress);
  }

  public List<Whitelist> getWhitelistConfig(String accountId) {
    WhitelistConfig value;

    // Cache should never be null, but just in case
    if (whitelistConfigCache == null) {
      value = getWhitelistConfigFromDB(accountId);
      return value.getWhitelistList();
    }

    value = whitelistConfigCache.get(accountId);
    if (value == null) {
      value = getWhitelistConfigFromDB(accountId);
      whitelistConfigCache.put(accountId, value);
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
    whitelistConfigCache.remove(accountId);
  }

  private boolean checkWhitelist(List<Whitelist> whitelistConfigList, String ipAddress) {
    if (isEmpty(whitelistConfigList) || (getInstance().isValid(ipAddress) && isLocalAddress(ipAddress))) {
      return true;
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
            log.warn("Exception while checking if the ip {} is in range: {}", ipAddress, condition);
            return false;
          }
        }
      } else {
        return ipAddress.matches(condition);
      }
    });
  }

  private boolean isLocalAddress(String ip) {
    try {
      InetAddress address = InetAddress.getByName(ip);
      return address.isAnyLocalAddress() || address.isLoopbackAddress()
          || NetworkInterface.getByInetAddress(address) != null;
    } catch (SocketException | UnknownHostException e) {
      log.warn("Unexpected Error", e);
      return false;
    }
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
    log.info("Updated whitelist config {} for account {}", whitelist.getUuid(), whitelist.getAccountId());
    auditServiceHelper.reportForAuditingUsingAccountId(whitelist.getAccountId(), null, whitelist, Type.UPDATE);
    return get(whitelist.getAccountId(), whitelist.getUuid());
  }

  @Override
  public boolean delete(String accountId, String whitelistId) {
    Whitelist whitelist = get(accountId, whitelistId);
    notNullCheck("whitelist", whitelist);
    Query<Whitelist> whitelistQuery = wingsPersistence.createQuery(Whitelist.class)
                                          .filter(Whitelist.ACCOUNT_ID_KEY, accountId)
                                          .filter(ID_KEY, whitelistId);
    boolean delete = wingsPersistence.delete(whitelistQuery);
    if (delete) {
      evictWhitelistConfigCache(whitelist.getAccountId());
      log.info("Deleted whitelist config {} for account {}", whitelistId, accountId);
      auditServiceHelper.reportDeleteForAuditingUsingAccountId(whitelist.getAccountId(), whitelist);
    }
    return delete;
  }

  @Override
  public boolean deleteAll(String accountId) {
    deleteByAccountId(accountId);
    return true;
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
