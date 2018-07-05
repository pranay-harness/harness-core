package software.wings.service.impl;

import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.MongoHelper.setUnset;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.DeploymentPreference;
import software.wings.beans.Preference;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.PreferenceService;
import software.wings.utils.Misc;

@Singleton
public class PreferenceServiceImpl implements PreferenceService {
  private static final Logger logger = LoggerFactory.getLogger(PreferenceServiceImpl.class);
  public static final String USER_ID_KEY = "userId";
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public Preference save(String accountId, String userId, Preference preference) {
    Preference savedPreference = null;
    try {
      savedPreference = wingsPersistence.saveAndGet(Preference.class, preference);
    } catch (Exception e) {
      logger.error("Exception while saving preference to DB: ", Misc.getMessage(e));
    }
    return savedPreference;
  }

  @Override
  public Preference get(String accountId, String userId, String preferenceId) {
    return wingsPersistence.createQuery(Preference.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(USER_ID_KEY, userId)
        .filter(Base.ID_KEY, preferenceId)
        .get();
  }

  @Override
  public PageResponse<Preference> list(PageRequest<Preference> pageRequest, String userId) {
    pageRequest.addFilter(USER_ID_KEY, EQ, userId);
    return wingsPersistence.query(Preference.class, pageRequest);
  }

  @Override
  public Preference update(String accountId, String userId, String preferenceId, Preference preference) {
    // Update preference for given account, user and preference Id
    DeploymentPreference deployPref = null;

    if (preference instanceof DeploymentPreference) {
      deployPref = (DeploymentPreference) preference;

      UpdateOperations<Preference> updateOperations = wingsPersistence.createUpdateOperations(Preference.class);
      // Set fields to update
      setUnset(updateOperations, "name", deployPref.getName());
      setUnset(updateOperations, "appIds", deployPref.getAppIds());
      setUnset(updateOperations, "pipelineIds", deployPref.getPipelineIds());
      setUnset(updateOperations, "workflowIds", deployPref.getWorkflowIds());
      setUnset(updateOperations, "serviceIds", deployPref.getServiceIds());
      setUnset(updateOperations, "envIds", deployPref.getEnvIds());
      setUnset(updateOperations, "status", deployPref.getStatus());
      setUnset(updateOperations, "startTime", deployPref.getStartTime());
      setUnset(updateOperations, "endTime", deployPref.getEndTime());

      wingsPersistence.update(wingsPersistence.createQuery(Preference.class)
                                  .filter(ACCOUNT_ID_KEY, accountId)
                                  .filter(USER_ID_KEY, userId)
                                  .filter(Base.ID_KEY, preferenceId),
          updateOperations);
    }

    // Return updated preference
    return wingsPersistence.get(Preference.class, Base.ID_KEY);
  }

  @Override
  public void delete(String accountId, String userId, String preferenceId) {
    wingsPersistence.delete(wingsPersistence.createQuery(Preference.class)
                                .filter(ACCOUNT_ID_KEY, accountId)
                                .filter(USER_ID_KEY, userId)
                                .filter(Base.ID_KEY, preferenceId));
  }
}
