package software.wings.service.impl.analysis;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute.Action;
import software.wings.service.intfc.AuthService;
import software.wings.sm.ExecutionStatus;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
public class ContinuousVerificationServiceImpl implements ContinuousVerificationService {
  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected AuthService authService;
  private static final Logger logger = LoggerFactory.getLogger(ContinuousVerificationServiceImpl.class);

  @Override
  public void saveCVExecutionMetaData(ContinuousVerificationExecutionMetaData continuousVerificationExecutionMetaData) {
    wingsPersistence.save(continuousVerificationExecutionMetaData);
  }

  @Override
  public void setMetaDataExecutionStatus(String stateExecutionId, ExecutionStatus status) {
    Query<ContinuousVerificationExecutionMetaData> query =
        wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class)
            .filter("stateExecutionId", stateExecutionId);

    wingsPersistence.update(query,
        wingsPersistence.createUpdateOperations(ContinuousVerificationExecutionMetaData.class)
            .set("executionStatus", status));
  }

  @Override
  public LinkedHashMap<Long,
      LinkedHashMap<String,
          LinkedHashMap<String,
              LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
  getCVExecutionMetaData(String accountId, long beginEpochTs, long endEpochTs, final User user) throws ParseException {
    LinkedHashMap<Long,
        LinkedHashMap<String,
            LinkedHashMap<String,
                LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>> results =
        new LinkedHashMap<>();
    if (user == null) {
      // user is null, we can't validate permissions. Returning empty.
      logger.warn("Returning empty results from getCVExecutionMetaData since user was null");
      return results;
    }
    List<ContinuousVerificationExecutionMetaData> continuousVerificationExecutionMetaData =
        wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class)
            .filter("accountId", accountId)
            .field("workflowStartTs")
            .greaterThanOrEq(beginEpochTs)
            .field("workflowStartTs")
            .lessThan(endEpochTs)
            .field("applicationId")
            .in(getAllowedApplicationsForUser(user, accountId))
            .order("-workflowStartTs, stateStartTs")
            .asList();

    Map<String, Long> pipelineTimeStampMap = new HashMap<>();

    for (ContinuousVerificationExecutionMetaData executionMetaData : continuousVerificationExecutionMetaData) {
      if (executionMetaData.getPipelineId() != null) {
        if (!pipelineTimeStampMap.containsKey(executionMetaData.getPipelineId())) {
          pipelineTimeStampMap.put(executionMetaData.getPipelineId(), executionMetaData.getWorkflowStartTs());
        } else if (executionMetaData.getWorkflowStartTs()
            > pipelineTimeStampMap.get(executionMetaData.getPipelineId())) {
          pipelineTimeStampMap.put(executionMetaData.getPipelineId(), executionMetaData.getWorkflowStartTs());
        }
      }
    }

    long startTimeTs;

    continuousVerificationExecutionMetaData =
        validatePermissionsAndGetAllowedExecutionList(user, accountId, continuousVerificationExecutionMetaData);
    for (ContinuousVerificationExecutionMetaData executionMetaData : continuousVerificationExecutionMetaData) {
      if (executionMetaData.getPipelineId() != null) {
        startTimeTs = pipelineTimeStampMap.get(executionMetaData.getPipelineId());
      } else {
        startTimeTs = executionMetaData.getWorkflowStartTs();
      }
      startTimeTs = Instant.ofEpochMilli(startTimeTs).truncatedTo(ChronoUnit.DAYS).toEpochMilli();
      if (!results.containsKey(startTimeTs)) {
        results.put(startTimeTs, new LinkedHashMap<>());
      }

      if (!results.get(startTimeTs).containsKey(executionMetaData.getArtifactName())) {
        results.get(startTimeTs).put(executionMetaData.getArtifactName(), new LinkedHashMap<>());
      }

      String envWorkflowName = executionMetaData.getEnvName() + "/" + executionMetaData.getWorkflowName();
      if (!results.get(startTimeTs).get(executionMetaData.getArtifactName()).containsKey(envWorkflowName)) {
        results.get(startTimeTs).get(executionMetaData.getArtifactName()).put(envWorkflowName, new LinkedHashMap<>());
      }

      if (!results.get(startTimeTs)
               .get(executionMetaData.getArtifactName())
               .get(envWorkflowName)
               .containsKey(executionMetaData.getWorkflowExecutionId())) {
        results.get(startTimeTs)
            .get(executionMetaData.getArtifactName())
            .get(envWorkflowName)
            .put(executionMetaData.getWorkflowExecutionId(), new LinkedHashMap<>());
      }

      String phaseName = executionMetaData.getPhaseName() == null ? "BASIC" : executionMetaData.getPhaseName();

      if (!results.get(startTimeTs)
               .get(executionMetaData.getArtifactName())
               .get(envWorkflowName)
               .get(executionMetaData.getWorkflowExecutionId())
               .containsKey(phaseName)) {
        results.get(startTimeTs)
            .get(executionMetaData.getArtifactName())
            .get(envWorkflowName)
            .get(executionMetaData.getWorkflowExecutionId())
            .put(phaseName, new ArrayList<>());
      }
      results.get(startTimeTs)
          .get(executionMetaData.getArtifactName())
          .get(envWorkflowName)
          .get(executionMetaData.getWorkflowExecutionId())
          .get(phaseName)
          .add(executionMetaData);
    }

    return results;
  }

  /**
   * Check if the user has permissions to view the executionData.
   * @param user
   * @param accountId
   * @param executionMetaDataList
   * @return true if user has all the required permissions, false otherwise.
   */
  private List<ContinuousVerificationExecutionMetaData> validatePermissionsAndGetAllowedExecutionList(final User user,
      final String accountId, final List<ContinuousVerificationExecutionMetaData> executionMetaDataList) {
    List<ContinuousVerificationExecutionMetaData> finalList = new ArrayList<>();
    Map<String, AppPermissionSummary> userAppPermissions =
        authService.getUserPermissionInfo(accountId, user).getAppPermissionMapInternal();
    //"Cache" it by applicationId.
    Map<String, Set<String>> servicePermissionsByApp = new HashMap<>();
    Map<String, Set<String>> pipelinePermissionsByApp = new HashMap<>();
    Map<String, Set<String>> workflowPermissionsByApp = new HashMap<>();
    Map<String, Set<String>> envPermissionsByApp = new HashMap<>();

    for (ContinuousVerificationExecutionMetaData executionMetaData : executionMetaDataList) {
      final String applicationId = executionMetaData.getApplicationId();
      Set<String> servicePermissions, pipelinePermissions, wfPermissions, envPermissions;

      if (!servicePermissionsByApp.containsKey(applicationId)) {
        // If it's  not present for servicePermissions,it's not present for anything. So fill up the map.
        servicePermissionsByApp.put(
            applicationId, userAppPermissions.get(applicationId).getServicePermissions().get(Action.READ));
        pipelinePermissionsByApp.put(
            applicationId, userAppPermissions.get(applicationId).getPipelinePermissions().get(Action.READ));
        envPermissionsByApp.put(
            applicationId, userAppPermissions.get(applicationId).getEnvPermissions().get(Action.READ));
        workflowPermissionsByApp.put(
            applicationId, userAppPermissions.get(applicationId).getWorkflowPermissions().get(Action.READ));
      }
      servicePermissions = servicePermissionsByApp.get(applicationId);
      pipelinePermissions = pipelinePermissionsByApp.get(applicationId);
      envPermissions = envPermissionsByApp.get(applicationId);
      wfPermissions = workflowPermissionsByApp.get(applicationId);
      logger.info("Service permissions for user {} are {}", user.getName(), servicePermissions);
      logger.info("Pipeline permissions for user {} are {}", user.getName(), pipelinePermissions);
      logger.info("environment permissions for user {} are {}", user.getName(), envPermissions);
      logger.info("workflow permissions for user {} are {}", user.getName(), wfPermissions);

      if (checkIfPermissionsApproved(servicePermissions, executionMetaData.getServiceId())
          && checkIfPermissionsApproved(pipelinePermissions, executionMetaData.getPipelineId())
          && checkIfPermissionsApproved(wfPermissions, executionMetaData.getWorkflowId())
          && checkIfPermissionsApproved(envPermissions, executionMetaData.getEnvId())) {
        finalList.add(executionMetaData);
      } else {
        logger.info("User {} does not have permissions to view the execution data {} and {} and {} and {}",
            user.getName(), executionMetaData.getServiceName(), executionMetaData.getWorkflowName(),
            executionMetaData.getEnvName(), executionMetaData.getPipelineName());
        logger.info("User {} does not have permissions to view the execution data {} and {} and {} and {}",
            user.getName(), executionMetaData.getServiceId(), executionMetaData.getWorkflowId(),
            executionMetaData.getEnvId(), executionMetaData.getPipelineId());
      }
    }
    return finalList;
  }

  /**
   *
   * @param setToCheck
   * @param value
   * @return False if set is either empty or it does not contain value. True otherwise.
   */
  private boolean checkIfPermissionsApproved(final Set<String> setToCheck, final String value) {
    logger.info("Validating with Set: {} and value: {}", setToCheck, value);
    if (EmptyPredicate.isEmpty(value)) {
      return true;
    }

    if (EmptyPredicate.isEmpty(setToCheck) || !setToCheck.contains(value)) {
      logger.info("Permissions rejected for value {} in set {}", value, setToCheck);
      return false;
    }
    return true;
  }

  private List<String> getAllowedApplicationsForUser(final User user, final String accountId) {
    Map<String, AppPermissionSummary> userApps =
        authService.getUserPermissionInfo(accountId, user).getAppPermissionMapInternal();
    logger.info("Applications allowed for user {} are {}", user.getName(), userApps.keySet());
    return new ArrayList<>(userApps.keySet());
  }
}
