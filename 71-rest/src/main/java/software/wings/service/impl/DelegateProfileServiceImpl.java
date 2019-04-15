package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateProfile;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateProfileService;

import java.util.List;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rishi on 7/31/18
 */
@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateProfileServiceImpl implements DelegateProfileService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<DelegateProfile> list(PageRequest<DelegateProfile> pageRequest) {
    return wingsPersistence.query(DelegateProfile.class, pageRequest);
  }

  @Override
  public DelegateProfile get(String accountId, String delegateProfileId) {
    return wingsPersistence.createQuery(DelegateProfile.class)
        .filter(DelegateProfile.ACCOUNT_ID_KEY, accountId)
        .filter(DelegateProfile.ID_KEY, delegateProfileId)
        .get();
  }

  @Override
  public DelegateProfile update(DelegateProfile delegateProfile) {
    UpdateOperations<DelegateProfile> updateOperations = wingsPersistence.createUpdateOperations(DelegateProfile.class);
    setUnset(updateOperations, "name", delegateProfile.getName());
    setUnset(updateOperations, "description", delegateProfile.getDescription());
    setUnset(updateOperations, "startupScript", delegateProfile.getStartupScript());

    Query<DelegateProfile> query = wingsPersistence.createQuery(DelegateProfile.class)
                                       .filter("accountId", delegateProfile.getAccountId())
                                       .filter(ID_KEY, delegateProfile.getUuid());
    wingsPersistence.update(query, updateOperations);
    DelegateProfile updatedDelegateProfile = get(delegateProfile.getAccountId(), delegateProfile.getUuid());
    logger.info("Updated delegate profile: {}", updatedDelegateProfile.getUuid());

    return updatedDelegateProfile;
  }

  @Override
  public DelegateProfile add(DelegateProfile delegateProfile) {
    delegateProfile.setAppId(GLOBAL_APP_ID);
    DelegateProfile persistedProfile = wingsPersistence.saveAndGet(DelegateProfile.class, delegateProfile);
    logger.info("Added delegate profile: {}", persistedProfile.getUuid());
    return persistedProfile;
  }

  @Override
  public void delete(String accountId, String delegateProfileId) {
    DelegateProfile delegateProfile = wingsPersistence.createQuery(DelegateProfile.class)
                                          .filter("accountId", accountId)
                                          .filter(ID_KEY, delegateProfileId)
                                          .get();
    if (delegateProfile != null) {
      ensureProfileSafeToDelete(accountId, delegateProfile);
      logger.info("Deleting delegate profile: {}", delegateProfileId);
      wingsPersistence.delete(delegateProfile);
    }
  }

  private void ensureProfileSafeToDelete(String accountId, DelegateProfile delegateProfile) {
    String delegateProfileId = delegateProfile.getUuid();
    List<Delegate> delegates = wingsPersistence.createQuery(Delegate.class).filter("accountId", accountId).asList();
    List<String> delegateNames = delegates.stream()
                                     .filter(delegate -> delegate.getDelegateProfileId() == delegateProfileId)
                                     .map(Delegate::getHostName)
                                     .collect(toList());
    if (isNotEmpty(delegateNames)) {
      String message = format("Delegate profile [%s] could not be deleted because it's used by these delegates [%s]",
          delegateProfile.getName(), Joiner.on(", ").join(delegateNames));
      throw new InvalidRequestException(message, USER);
    }
  }
}
