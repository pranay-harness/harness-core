package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.beans.DelegateTask.Status.ABORTED;
import static io.harness.beans.DelegateTask.Status.ERROR;
import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.beans.DelegateTask.Status.STARTED;
import static io.harness.beans.DelegateTask.Status.runningStatuses;
import static io.harness.beans.FeatureName.GIT_HOST_CONNECTIVITY;
import static io.harness.beans.FeatureName.PER_AGENT_CAPABILITIES;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.SizeFunction.size;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.DelegateTaskAbortEvent.Builder.aDelegateTaskAbortEvent;
import static io.harness.delegate.beans.DelegateTaskEvent.DelegateTaskEventBuilder.aDelegateTaskEvent;
import static io.harness.delegate.beans.executioncapability.ExecutionCapability.EvaluationMode;
import static io.harness.delegate.task.TaskFailureReason.EXPIRED;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;
import static io.harness.govern.Switch.noop;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.alert.AlertType.NoEligibleDelegates;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.beans.FeatureName;
import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilityRequirement.CapabilityRequirementKeys;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionKeys;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.capability.CapabilityTaskSelectionDetails.CapabilityTaskSelectionDetailsKeys;
import io.harness.capability.internal.CapabilityAttributes;
import io.harness.capability.service.CapabilityService;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.delegate.beans.DelegateProgressData;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskAbortEvent;
import io.harness.delegate.beans.DelegateTaskEvent;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskPackage.DelegateTaskPackageBuilder;
import io.harness.delegate.beans.DelegateTaskRank;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.DelegateTaskResponse.ResponseCode;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.NoAvailableDelegatesException;
import io.harness.delegate.beans.NoInstalledDelegatesException;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.SecretDetail;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.TaskSelectorMap;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.delegate.task.TaskLogContext;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskParameters;
import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskResponse;
import io.harness.delegate.task.executioncapability.CapabilityCheckDetails;
import io.harness.environment.SystemEnvironment;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.CriticalExpressionEvaluationException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.FailureType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionReflectionUtils;
import io.harness.ff.FeatureFlagService;
import io.harness.lock.PersistentLocker;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.logstreaming.LogStreamingServiceRestClient;
import io.harness.mongo.DelayLogContext;
import io.harness.network.SafeHttpCall;
import io.harness.observer.Subject;
import io.harness.persistence.HPersistence;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateCache;
import io.harness.service.intfc.DelegateCallbackRegistry;
import io.harness.service.intfc.DelegateCallbackService;
import io.harness.service.intfc.DelegateInsightsService;
import io.harness.service.intfc.DelegateSetupService;
import io.harness.service.intfc.DelegateSyncService;
import io.harness.service.intfc.DelegateTaskResultsProvider;
import io.harness.service.intfc.DelegateTaskSelectorMapService;
import io.harness.service.intfc.DelegateTaskService;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.app.MainConfiguration;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.GitConfig;
import software.wings.beans.GitValidationParameters;
import software.wings.beans.HostValidationTaskParameters;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.beans.alert.NoEligibleDelegatesAlert;
import software.wings.beans.alert.NoInstalledDelegatesAlert;
import software.wings.common.AuditHelper;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.delegatetasks.cv.RateLimitExceededException;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.expression.ManagerPreExecutionExpressionEvaluator;
import software.wings.expression.ManagerPreviewExpressionEvaluator;
import software.wings.expression.NgSecretManagerFunctor;
import software.wings.expression.SecretManagerFunctor;
import software.wings.expression.SecretManagerMode;
import software.wings.expression.SweepingOutputSecretFunctor;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandTaskParameters;
import software.wings.helpers.ext.pcf.request.PcfCommandTaskParameters.PcfCommandTaskParametersBuilder;
import software.wings.helpers.ext.pcf.request.PcfRunPluginCommandRequest;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.impl.infra.InfraDownloadService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.DelegateTaskServiceClassic;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.atmosphere.cpr.BroadcasterFactory;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@ValidateOnExecution
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@BreakDependencyOn("io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander")
@BreakDependencyOn("software.wings.helpers.ext.pcf.request.PcfCommandRequest")
@BreakDependencyOn("software.wings.helpers.ext.pcf.request.PcfCommandTaskParameters")
@OwnedBy(DEL)
public class DelegateTaskServiceClassicImpl implements DelegateTaskServiceClassic {
  private static final String ASYNC = "async";
  private static final String SYNC = "sync";
  private static final String STREAM_DELEGATE = "/stream/delegate/";
  public static final String TASK_SELECTORS = "Task Selectors";
  public static final String TASK_CATEGORY_MAP = "Task Category Map";
  private static final long CAPABILITIES_CHECK_TASK_TIMEOUT_IN_MINUTES = 1L;

  private static final long VALIDATION_TIMEOUT = TimeUnit.SECONDS.toMillis(12);

  @Inject private HPersistence persistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private EventEmitter eventEmitter;
  @Inject private BroadcasterFactory broadcasterFactory;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private AlertService alertService;
  @Inject private Clock clock;
  @Inject private VersionInfoManager versionInfoManager;
  @Inject private Injector injector;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private InfraDownloadService infraDownloadService;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private SecretManager secretManager;
  @Inject private ExpressionEvaluator evaluator;
  @Inject private FileService fileService;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private ConfigService configService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private PersistentLocker persistentLocker;
  @Inject private DelegateTaskBroadcastHelper broadcastHelper;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private SubdomainUrlHelperIntfc subdomainUrlHelper;
  @Inject private ConfigurationController configurationController;
  @Inject private DelegateSelectionLogsService delegateSelectionLogsService;
  @Inject private DelegateConnectionDao delegateConnectionDao;
  @Inject private SystemEnvironment sysenv;
  @Inject private DelegateSyncService delegateSyncService;
  @Inject private DelegateTaskService delegateTaskService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private DelegateCallbackRegistry delegateCallbackRegistry;
  @Inject private DelegateTaskSelectorMapService taskSelectorMapService;
  @Inject private SettingsService settingsService;
  @Inject private LogStreamingServiceRestClient logStreamingServiceRestClient;
  @Inject private SecretManagerClientService ngSecretService;
  @Inject private DelegateCache delegateCache;
  @Inject private CapabilityService capabilityService;
  @Inject private DelegateInsightsService delegateInsightsService;
  @Inject private DelegateSetupService delegateSetupService;
  @Inject private AuditHelper auditHelper;

  @Inject @Getter private Subject<DelegateObserver> subject = new Subject<>();

  private Supplier<Long> taskCountCache = Suppliers.memoizeWithExpiration(this::fetchTaskCount, 1, TimeUnit.MINUTES);

  private LoadingCache<String, String> logStreamingAccountTokenCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build(new CacheLoader<String, String>() {
            @Override
            public String load(String accountId) throws IOException {
              return retrieveLogStreamingAccountToken(accountId);
            }
          });

  public static void embedCapabilitiesInDelegateTask(
      DelegateTask task, Collection<EncryptionConfig> encryptionConfigs, ExpressionEvaluator maskingEvaluator) {
    if (isEmpty(task.getData().getParameters()) || isNotEmpty(task.getExecutionCapabilities())) {
      return;
    }

    task.setExecutionCapabilities(new ArrayList<>());
    task.getExecutionCapabilities().addAll(
        Arrays.stream(task.getData().getParameters())
            .filter(param -> param instanceof ExecutionCapabilityDemander)
            .flatMap(param
                -> ((ExecutionCapabilityDemander) param).fetchRequiredExecutionCapabilities(maskingEvaluator).stream())
            .collect(toList()));

    if (isNotEmpty(encryptionConfigs)) {
      task.getExecutionCapabilities().addAll(
          EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForSecretManagers(
              encryptionConfigs, maskingEvaluator));
    }
  }

  @VisibleForTesting
  protected void checkTaskRankRateLimit(DelegateTaskRank rank) {
    if (rank == null) {
      rank = DelegateTaskRank.CRITICAL;
    }

    if (rankLimitReached(rank)) {
      throw new RateLimitExceededException("Rate limit reached for tasks with rank " + rank.name());
    }
  }

  private boolean rankLimitReached(DelegateTaskRank rank) {
    Long totalTaskCount = taskCountCache.get();
    return totalTaskCount >= obtainRankLimit(rank);
  }

  private long obtainRankLimit(DelegateTaskRank rank) {
    switch (rank) {
      case OPTIONAL:
        return mainConfiguration.getPortal().getOptionalDelegateTaskRejectAtLimit();
      case IMPORTANT:
        return mainConfiguration.getPortal().getImportantDelegateTaskRejectAtLimit();
      case CRITICAL:
        return mainConfiguration.getPortal().getCriticalDelegateTaskRejectAtLimit();
      default:
        throw new InvalidArgumentsException("Unsupported delegate task rank level " + rank);
    }
  }

  @VisibleForTesting
  @Override
  public void convertToExecutionCapability(DelegateTask task) {
    Set<ExecutionCapability> selectorCapabilities = new HashSet<>();

    if (isNotEmpty(task.getTags())) {
      SelectorCapability selectorCapability =
          SelectorCapability.builder().selectors(new HashSet<>(task.getTags())).selectorOrigin(TASK_SELECTORS).build();
      selectorCapabilities.add(selectorCapability);
    }

    if (task.getData() != null && task.getData().getTaskType() != null) {
      TaskGroup taskGroup = TaskType.valueOf(task.getData().getTaskType()).getTaskGroup();
      TaskSelectorMap mapFromTaskType = taskSelectorMapService.get(task.getAccountId(), taskGroup);
      if (mapFromTaskType != null && isNotEmpty(mapFromTaskType.getSelectors())) {
        SelectorCapability selectorCapability = SelectorCapability.builder()
                                                    .selectors(mapFromTaskType.getSelectors())
                                                    .selectorOrigin(TASK_CATEGORY_MAP)
                                                    .build();
        selectorCapabilities.add(selectorCapability);
      }
    }

    if (task.getExecutionCapabilities() == null) {
      task.setExecutionCapabilities(new ArrayList<>(selectorCapabilities));
    } else {
      task.getExecutionCapabilities().addAll(selectorCapabilities);
    }
  }

  @VisibleForTesting
  @Override
  public void saveDelegateTask(DelegateTask task, DelegateTask.Status taskStatus) {
    task.setStatus(taskStatus);
    task.setVersion(getVersion());
    task.setLastBroadcastAt(clock.millis());

    // For forward compatibility set the wait id to the uuid
    if (task.getUuid() == null) {
      task.setUuid(generateUuid());
    }

    if (task.getWaitId() == null) {
      task.setWaitId(task.getUuid());
    }
    // For backward compatibility we base the queue task expiry on the execution timeout
    if (task.getExpiry() == 0) {
      task.setExpiry(currentTimeMillis() + task.getData().getTimeout());
    }

    if (isBlank(task.getMustExecuteOnDelegateId())) {
      // order of these three calls is important, first capabilities are created, then appended, then used in
      // pickFirstAttemptDelegate
      generateCapabilitiesForTaskIfFeatureEnabled(task);
      convertToExecutionCapability(task);
      upsertCapabilityRequirements(task);
      task.setPreAssignedDelegateId(obtainCapableDelegateId(task, Collections.emptySet()));
    } else {
      task.setPreAssignedDelegateId(task.getMustExecuteOnDelegateId());
    }

    // Ensure that broadcast happens at least 5 seconds from current time for async tasks
    if (task.getData().isAsync()) {
      task.setNextBroadcast(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5));
    }

    checkTaskRankRateLimit(task.getRank());

    // TODO: Make this call to make sure there are no secrets in disallowed expressions
    // resolvePreAssignmentExpressions(task, CHECK_FOR_SECRETS);

    // Added temporarily to help identifying tasks whose task setup abstractions need to be fixed
    verifyTaskSetupAbstractions(task);

    persistence.save(task);
  }

  public String obtainCapableDelegateId(DelegateTask task, Set<String> alreadyTriedDelegates) {
    try (TaskLogContext ignore = new TaskLogContext(task.getUuid(), OVERRIDE_ERROR);
         AccountLogContext ignore2 = new AccountLogContext(task.getAccountId(), OVERRIDE_ERROR)) {
      if (!featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, task.getAccountId())) {
        // Old way with rebroadcasting
        return assignDelegateService.pickFirstAttemptDelegate(task);
      }

      BatchDelegateSelectionLog batch = delegateSelectionLogsService.createBatch(task);
      List<String> activeDelegates = assignDelegateService.retrieveActiveDelegates(task.getAccountId(), batch);
      delegateSelectionLogsService.save(batch);

      if (isEmpty(task.getExecutionCapabilities())) {
        return pickDelegateForTaskWithoutAnyAgentCapabilities(task, activeDelegates);
      }

      // get all agent capabilities and convert to CR
      List<ExecutionCapability> agentCapabilities =
          task.getExecutionCapabilities()
              .stream()
              .filter(capability -> EvaluationMode.AGENT == capability.evaluationMode())
              .collect(toList());

      if (isEmpty(agentCapabilities)) {
        return pickDelegateForTaskWithoutAnyAgentCapabilities(task, activeDelegates);
      }

      List<CapabilityRequirement> capabilityRequirements =
          createCapabilityRequirementInstances(task.getAccountId(), agentCapabilities);

      // get delegates capable to execute the task. Retry to cover case when there are no known delegates capable to do
      // the task and we are waiting for immediate capabilities validation
      Set<String> capableDelegateIds =
          capabilityService.getCapableDelegateIds(task.getAccountId(), capabilityRequirements);
      int i = 1;
      while (capableDelegateIds.isEmpty() && i <= 10) {
        sleep(ofSeconds(1));
        capableDelegateIds = capabilityService.getCapableDelegateIds(task.getAccountId(), capabilityRequirements);
        i++;
      }

      boolean ignoreAlreadyTriedDelegates =
          alreadyTriedDelegates == null || alreadyTriedDelegates.containsAll(capableDelegateIds);

      // Filter delegate to try different ones when rebroadcasting, but allow all eventually when all are exhausted
      Set<String> validDelegateIds =
          capableDelegateIds.stream()
              .filter(delegateId -> ignoreAlreadyTriedDelegates || !alreadyTriedDelegates.contains(delegateId))
              .collect(Collectors.toSet());

      // pick one, check still in scope and assign if ok or delete permission record and try another one
      for (String delegateId : validDelegateIds) {
        boolean assignableDelegate = true;
        for (CapabilityRequirement capabilityRequirement : capabilityRequirements) {
          if (!isDelegateStillInScope(task.getAccountId(), delegateId, capabilityRequirement.getUuid())) {
            capabilityService.deleteCapabilitySubjectPermission(
                task.getAccountId(), delegateId, capabilityRequirement.getUuid());
            assignableDelegate = false;
            break;
          }
        }

        if (assignableDelegate && activeDelegates.contains(delegateId)) {
          log.info("Setting preAssignedDelegate to {}.", delegateId);
          return delegateId;
        }
      }

      // No in scope delegates, capable of doing the task
      return null;
    } catch (Exception ex) {
      log.error("Unexpected error occurred while obtaining capable delegate Ids", ex);
      return null;
    }
  }

  @VisibleForTesting
  public String pickDelegateForTaskWithoutAnyAgentCapabilities(DelegateTask task, List<String> activeDelegates) {
    if (isEmpty(activeDelegates)) {
      log.warn("No active delegates found to execute the task.");
      return null;
    }

    boolean ignoreAlreadyTriedDelegates =
        task.getAlreadyTriedDelegates() == null || task.getAlreadyTriedDelegates().containsAll(activeDelegates);

    // Filter delegate to try different ones when rebroadcasting, but allow all eventually when all are exhausted
    Set<String> validDelegateIds =
        activeDelegates.stream()
            .filter(delegateId -> ignoreAlreadyTriedDelegates || !task.getAlreadyTriedDelegates().contains(delegateId))
            .collect(Collectors.toSet());

    for (String delegateId : validDelegateIds) {
      BatchDelegateSelectionLog batch = delegateSelectionLogsService.createBatch(task);
      boolean canAssign = assignDelegateService.canAssign(batch, delegateId, task);
      delegateSelectionLogsService.save(batch);

      if (canAssign) {
        log.info("Setting preAssignedDelegate for task without agent capabilities to {}.", delegateId);
        return delegateId;
      }
    }

    log.warn("No assignable active delegates found to execute the task.");
    return null;
  }

  @VisibleForTesting
  public void upsertCapabilityRequirements(DelegateTask task) {
    if (featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, task.getAccountId())
        && isNotEmpty(task.getExecutionCapabilities())) {
      // Check if any capability with AGENT evaluation mode is present
      List<ExecutionCapability> agentCapabilities =
          task.getExecutionCapabilities()
              .stream()
              .filter(capability -> EvaluationMode.AGENT == capability.evaluationMode())
              .collect(toList());

      if (isNotEmpty(agentCapabilities)) {
        List<Delegate> accountDelegates = assignDelegateService.getAccountDelegates(task.getAccountId());
        if (accountDelegates != null) {
          BatchDelegateSelectionLog batch = delegateSelectionLogsService.createBatch(task);
          List<String> assignableDelegateIds =
              accountDelegates.stream()
                  .filter(delegate
                      -> delegate.getStatus() != DelegateInstanceStatus.DELETED
                          && assignDelegateService.canAssign(batch, delegate.getUuid(), task))
                  .map(Delegate::getUuid)
                  .collect(Collectors.toList());
          delegateSelectionLogsService.save(batch);

          // for each of the agent capabilities, prepare CapabilityRequirement record
          List<CapabilityRequirement> capabilityRequirements =
              createCapabilityRequirementInstances(task.getAccountId(), agentCapabilities);

          // Process each of the CapabilityRequirement records to insert/update capability details, task selection
          // records and permissions
          for (CapabilityRequirement capabilityRequirement : capabilityRequirements) {
            CapabilityTaskSelectionDetails taskSelectionDetails =
                createCapabilityTaskSelectionDetailsInstance(task, capabilityRequirement, assignableDelegateIds);

            // This will wakeup iterator of BlockingCapabilityPermissionsRecordHandler to process blocking entries
            // urgently
            capabilityService.processTaskCapabilityRequirement(
                capabilityRequirement, taskSelectionDetails, assignableDelegateIds);
          }
        } else {
          log.info("No delegates found for the given account.");
        }
      } else {
        log.info("No AGENT execution capabilities found on task.");
      }
    } else {
      log.info("FF PER_AGENT_CAPABILITIES is disabled or task did not have any execution capabilities.");
    }
  }

  @VisibleForTesting
  public List<CapabilityRequirement> createCapabilityRequirementInstances(
      String accountId, List<ExecutionCapability> agentCapabilities) {
    List<CapabilityRequirement> capabilityRequirements = new ArrayList<>();
    for (ExecutionCapability agentCapability : agentCapabilities) {
      CapabilityRequirement capabilityRequirement =
          capabilityService.buildCapabilityRequirement(accountId, agentCapability);

      if (capabilityRequirement != null) {
        capabilityRequirements.add(capabilityRequirement);
      }
    }

    return capabilityRequirements;
  }

  /**
   * This method is intended to be used whenever we need to extract delegate selection related data from delegate task.
   * It assumes all data related to scoping and selectors
   */
  @VisibleForTesting
  public CapabilityTaskSelectionDetails createCapabilityTaskSelectionDetailsInstance(
      DelegateTask task, CapabilityRequirement capabilityRequirement, List<String> assignableDelegateIds) {
    // Get all selector capabilities(this already contains all task tags)
    List<SelectorCapability> selectorCapabilities = null;
    if (task.getExecutionCapabilities() != null) {
      selectorCapabilities = task.getExecutionCapabilities()
                                 .stream()
                                 .filter(c -> c instanceof SelectorCapability)
                                 .map(c -> (SelectorCapability) c)
                                 .collect(toList());
    }

    // TaskGroup is also required for scoping check
    TaskGroup taskGroup = task.getData() != null && isNotBlank(task.getData().getTaskType())
        ? TaskType.valueOf(task.getData().getTaskType()).getTaskGroup()
        : null;

    return capabilityService.buildCapabilityTaskSelectionDetails(
        capabilityRequirement, taskGroup, task.getSetupAbstractions(), selectorCapabilities, assignableDelegateIds);
  }

  @Override
  public void executeBatchCapabilityCheckTask(String accountId, String delegateId,
      List<CapabilitySubjectPermission> capabilitySubjectPermissions, String blockedTaskSelectionDetailsId) {
    List<CapabilityCheckDetails> capabilityCheckDetailsList =
        capabilitySubjectPermissions.stream()
            .map(capSubjectPermission -> {
              // Log that we did not revalidate the capability on time
              if (capSubjectPermission.getMaxValidUntil() > 0
                  && System.currentTimeMillis() > capSubjectPermission.getMaxValidUntil()) {
                log.warn("Capability {} is being re-validated with delay of {} millis.",
                    capSubjectPermission.getCapabilityId(),
                    System.currentTimeMillis() - capSubjectPermission.getMaxValidUntil());
              }

              // For re-validation cases we need to check that given delegate is still in scope for given capability and
              // remove record if it is not anymore. UNCHECKED and blocking ones are already checked prior to this
              if (isBlank(blockedTaskSelectionDetailsId)
                  && capSubjectPermission.getPermissionResult() != PermissionResult.UNCHECKED
                  && !isDelegateStillInScope(capSubjectPermission.getAccountId(), capSubjectPermission.getDelegateId(),
                      capSubjectPermission.getCapabilityId())) {
                capabilityService.deleteCapabilitySubjectPermission(capSubjectPermission.getUuid());
                return null;
              }

              CapabilityRequirement capabilityRequirement =
                  persistence.createQuery(CapabilityRequirement.class)
                      .filter(CapabilityRequirementKeys.accountId, capSubjectPermission.getAccountId())
                      .filter(CapabilityRequirementKeys.uuid, capSubjectPermission.getCapabilityId())
                      .get();

              if (capabilityRequirement != null && capabilityRequirement.getCapabilityParameters() != null
                  && isNotBlank(capabilityRequirement.getCapabilityType())) {
                return CapabilityCheckDetails.builder()
                    .accountId(capSubjectPermission.getAccountId())
                    .delegateId(capSubjectPermission.getDelegateId())
                    .capabilityId(capSubjectPermission.getCapabilityId())
                    .capabilityType(CapabilityType.valueOf(capabilityRequirement.getCapabilityType()))
                    .capabilityParameters(capabilityRequirement.getCapabilityParameters())
                    .build();
              }

              return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (isNotEmpty(capabilityCheckDetailsList)) {
      DelegateTask capabilitiesCheckTask =
          buildCapabilitiesCheckTask(accountId, delegateId, capabilityCheckDetailsList);

      try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR);
           AutoLogContext ignore1 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
        DelegateResponseData delegateResponseData = executeTask(capabilitiesCheckTask);

        if (delegateResponseData instanceof BatchCapabilityCheckTaskResponse) {
          BatchCapabilityCheckTaskResponse response = (BatchCapabilityCheckTaskResponse) delegateResponseData;

          for (CapabilityCheckDetails capabilityCheckDetails : response.getCapabilityCheckDetailsList()) {
            // Update permission record
            Query<CapabilitySubjectPermission> query =
                persistence.createQuery(CapabilitySubjectPermission.class)
                    .filter(CapabilitySubjectPermissionKeys.accountId, capabilityCheckDetails.getAccountId())
                    .filter(CapabilitySubjectPermissionKeys.delegateId, capabilityCheckDetails.getDelegateId())
                    .filter(CapabilitySubjectPermissionKeys.capabilityId, capabilityCheckDetails.getCapabilityId());

            UpdateOperations<CapabilitySubjectPermission> updateOperations =
                persistence.createUpdateOperations(CapabilitySubjectPermission.class);
            setUnset(updateOperations, CapabilitySubjectPermissionKeys.permissionResult,
                capabilityCheckDetails.getPermissionResult());
            setUnset(updateOperations, CapabilitySubjectPermissionKeys.maxValidUntil,
                System.currentTimeMillis()
                    + CapabilityAttributes.getValidityPeriod(capabilityCheckDetails.getCapabilityParameters())
                          .toMillis());
            setUnset(updateOperations, CapabilitySubjectPermissionKeys.revalidateAfter,
                System.currentTimeMillis()
                    + CapabilityAttributes
                          .getPeriodUntilNextValidation(capabilityCheckDetails.getCapabilityParameters())
                          .toMillis());

            persistence.findAndModify(query, updateOperations, HPersistence.returnNewOptions);

            if (isNotBlank(blockedTaskSelectionDetailsId)
                && capabilityCheckDetails.getPermissionResult() == PermissionResult.ALLOWED) {
              // Update task selection details record and mark it as not blocked
              Query<CapabilityTaskSelectionDetails> selectionDetailsQuery =
                  persistence.createQuery(CapabilityTaskSelectionDetails.class)
                      .filter(CapabilityTaskSelectionDetailsKeys.accountId, capabilityCheckDetails.getAccountId())
                      .filter(CapabilityTaskSelectionDetailsKeys.uuid, blockedTaskSelectionDetailsId);

              UpdateOperations<CapabilityTaskSelectionDetails> selectionDetailsUpdateOperations =
                  persistence.createUpdateOperations(CapabilityTaskSelectionDetails.class);
              setUnset(selectionDetailsUpdateOperations, CapabilityTaskSelectionDetailsKeys.blocked, false);

              persistence.findAndModify(
                  selectionDetailsQuery, selectionDetailsUpdateOperations, HPersistence.returnNewOptions);
            }
          }
        } else if ((delegateResponseData instanceof RemoteMethodReturnValueData)
            && (((RemoteMethodReturnValueData) delegateResponseData).getException()
                    instanceof InvalidRequestException)) {
          log.error("Invalid request exception: ", ((RemoteMethodReturnValueData) delegateResponseData).getException());
        } else {
          log.error("Batch capabilities check task execution got unexpected delegate response {}",
              delegateResponseData != null ? delegateResponseData.toString() : "null");
        }
      } catch (NoInstalledDelegatesException exception) {
        ignoredOnPurpose(exception);
      } catch (NoAvailableDelegatesException exception) {
        log.warn("Targeted delegate was not available for capabilities check task execution.", exception);
      } catch (Exception e) {
        log.error("Failed to execute capabilities check task.", e);
      }
    }
  }

  @VisibleForTesting
  public boolean isDelegateStillInScope(String accountId, String delegateId, String capabilityId) {
    List<CapabilityTaskSelectionDetails> taskSelectionDetailsList =
        capabilityService.getAllCapabilityTaskSelectionDetails(accountId, capabilityId);

    if (isEmpty(taskSelectionDetailsList)) {
      return true;
    }

    for (CapabilityTaskSelectionDetails taskSelectionDetails : taskSelectionDetailsList) {
      if (isDelegateInCapabilityScope(accountId, delegateId, taskSelectionDetails)) {
        return true;
      }
    }

    // Since the delegate is not in scope for given capability, we need to mark capability task selection details as
    // blocked, if no other delegates are in scope
    List<String> notDeniedDelegates = capabilityService.getNotDeniedCapabilityPermissions(accountId, capabilityId)
                                          .stream()
                                          .map(CapabilitySubjectPermission::getDelegateId)
                                          .collect(Collectors.toList());

    for (CapabilityTaskSelectionDetails taskSelectionDetails : taskSelectionDetailsList) {
      if (!notDeniedDelegates.stream().anyMatch(
              delegateIdentifier -> isDelegateInCapabilityScope(accountId, delegateIdentifier, taskSelectionDetails))) {
        // Update task selection details record and mark it as blocked
        Query<CapabilityTaskSelectionDetails> selectionDetailsQuery =
            persistence.createQuery(CapabilityTaskSelectionDetails.class)
                .filter(CapabilityTaskSelectionDetailsKeys.accountId, accountId)
                .filter(CapabilityTaskSelectionDetailsKeys.uuid, taskSelectionDetails.getUuid());

        UpdateOperations<CapabilityTaskSelectionDetails> selectionDetailsUpdateOperations =
            persistence.createUpdateOperations(CapabilityTaskSelectionDetails.class);
        setUnset(selectionDetailsUpdateOperations, CapabilityTaskSelectionDetailsKeys.blocked, true);

        persistence.findAndModify(
            selectionDetailsQuery, selectionDetailsUpdateOperations, HPersistence.returnNewOptions);
      }
    }

    return false;
  }

  @VisibleForTesting
  public boolean isDelegateInCapabilityScope(
      String accountId, String delegateId, CapabilityTaskSelectionDetails taskSelectionDetails) {
    List<ExecutionCapability> selectorCapabilities = new ArrayList<>();
    if (isNotEmpty(taskSelectionDetails.getTaskSelectors())) {
      taskSelectionDetails.getTaskSelectors().forEach(
          (origin, selectors)
              -> selectorCapabilities.add(SelectorCapability.builder()
                                              .capabilityType(CapabilityType.SELECTORS)
                                              .selectorOrigin(origin)
                                              .selectors(selectors)
                                              .build()));
    }

    String appId = null;
    String envId = null;
    String infraMappingId = null;
    if (isNotEmpty(taskSelectionDetails.getTaskSetupAbstractions())) {
      appId = taskSelectionDetails.getTaskSetupAbstractions().get(Cd1SetupFields.APP_ID_FIELD);
      envId = taskSelectionDetails.getTaskSetupAbstractions().get(Cd1SetupFields.ENV_ID_FIELD);
      infraMappingId =
          taskSelectionDetails.getTaskSetupAbstractions().get(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD);
    }

    return assignDelegateService.canAssign(null, delegateId, accountId, appId, envId, infraMappingId,
        taskSelectionDetails.getTaskGroup(), selectorCapabilities, taskSelectionDetails.getTaskSetupAbstractions());
  }

  @VisibleForTesting
  public DelegateTask buildCapabilitiesCheckTask(
      String accountId, String delegateId, List<CapabilityCheckDetails> capabilityCheckParamsList) {
    return DelegateTask.builder()
        .accountId(accountId)
        .rank(DelegateTaskRank.CRITICAL)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.BATCH_CAPABILITY_CHECK.name())
                  .parameters(new Object[] {BatchCapabilityCheckTaskParameters.builder()
                                                .capabilityCheckDetailsList(capabilityCheckParamsList)
                                                .build()})
                  .timeout(TimeUnit.MINUTES.toMillis(CAPABILITIES_CHECK_TASK_TIMEOUT_IN_MINUTES))
                  .build())
        .mustExecuteOnDelegateId(delegateId)
        .build();
  }

  private void verifyTaskSetupAbstractions(DelegateTask task) {
    if (isNotBlank(task.getUuid()) && task.getData() != null && task.getData().getTaskType() != null) {
      try (AutoLogContext ignore1 = new TaskLogContext(task.getUuid(), task.getData().getTaskType(),
               TaskType.valueOf(task.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_NESTS);) {
        // Verify presence of Environment type, if EnvironmentId is present
        if (isNotEmpty(task.getSetupAbstractions())
            && task.getSetupAbstractions().get(Cd1SetupFields.ENV_ID_FIELD) != null
            && task.getSetupAbstractions().get(Cd1SetupFields.ENV_TYPE_FIELD) == null) {
          log.error("Missing envType setup abstraction", new RuntimeException());
        }

        // Verify presence of ServiceId, if Infrastructure Mapping is present
        if (isNotEmpty(task.getSetupAbstractions())
            && task.getSetupAbstractions().get(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD) != null
            && task.getSetupAbstractions().get(Cd1SetupFields.SERVICE_ID_FIELD) == null) {
          log.error("Missing serviceId setup abstraction", new RuntimeException());
        }
      }
    }
  }

  private Long fetchTaskCount() {
    return persistence.createQuery(DelegateTask.class, excludeAuthority).count();
  }

  @Override
  public String queueParkedTask(String accountId, String taskId) {
    DelegateTask task = persistence.createQuery(DelegateTask.class)
                            .filter(DelegateTaskKeys.accountId, accountId)
                            .filter(DelegateTaskKeys.uuid, taskId)
                            .get();

    task.getData().setAsync(true);

    try (AutoLogContext ignore1 = new TaskLogContext(task.getUuid(), task.getData().getTaskType(),
             TaskType.valueOf(task.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_NESTS);
         AutoLogContext ignore2 = new AccountLogContext(task.getAccountId(), OVERRIDE_ERROR)) {
      saveDelegateTask(task, QUEUED);
      log.info("Queueing parked task");
      broadcastHelper.broadcastNewDelegateTaskAsync(task);
    }
    return task.getUuid();
  }

  @Override
  public byte[] getParkedTaskResults(String accountId, String taskId, String driverId) {
    DelegateTaskResultsProvider delegateTaskResultsProvider =
        delegateCallbackRegistry.obtainDelegateTaskResultsProvider(driverId);
    if (delegateTaskResultsProvider == null) {
      return new byte[0];
    }
    return delegateTaskResultsProvider.getDelegateTaskResults(taskId);
  }

  private void generateCapabilitiesForTaskIfFeatureEnabled(DelegateTask task) {
    addMergedParamsForCapabilityCheck(task);

    DelegateTaskPackage delegateTaskPackage = getDelegatePackageWithEncryptionConfig(task);
    embedCapabilitiesInDelegateTask(task,
        delegateTaskPackage == null || isEmpty(delegateTaskPackage.getEncryptionConfigs())
            ? emptyList()
            : delegateTaskPackage.getEncryptionConfigs().values(),
        new ManagerPreviewExpressionEvaluator());

    if (isNotEmpty(task.getExecutionCapabilities())) {
      log.info(CapabilityHelper.generateLogStringWithCapabilitiesGenerated(
          task.getData().getTaskType(), task.getExecutionCapabilities()));
    }
  }

  // For some of the tasks, the necessary factors to do capability check is split across multiple
  // params. So none of the params can provide the execution capability by itself. To work around this,
  // we're adding extra params that combines these split params.
  private void addMergedParamsForCapabilityCheck(DelegateTask task) {
    List<Object> newParams;
    TaskType type = TaskType.valueOf(task.getData().getTaskType());
    Object[] params = task.getData().getParameters();
    switch (type) {
      case HOST_VALIDATION:
        HostValidationTaskParameters hostValidationTaskParameters =
            HostValidationTaskParameters.builder()
                .hostNames((List<String>) params[2])
                .connectionSetting((SettingAttribute) params[3])
                .encryptionDetails((List<EncryptedDataDetail>) params[4])
                .executionCredential((ExecutionCredential) params[5])
                .build();
        newParams = new ArrayList<>(Arrays.asList(hostValidationTaskParameters));
        task.getData().setParameters(newParams.toArray());
        return;
      case PCF_COMMAND_TASK:
        PcfCommandRequest commandRequest = (PcfCommandRequest) params[0];
        if (!(commandRequest instanceof PcfRunPluginCommandRequest)) {
          PcfCommandTaskParametersBuilder parametersBuilder =
              PcfCommandTaskParameters.builder().pcfCommandRequest(commandRequest);
          if (params.length > 1) {
            parametersBuilder.encryptedDataDetails((List<EncryptedDataDetail>) params[1]);
          }
          newParams = new ArrayList<>(Collections.singletonList(parametersBuilder.build()));
          task.getData().setParameters(newParams.toArray());
        }
        return;
      case GIT_COMMAND:
        GitConfig config = (GitConfig) params[1];
        List<EncryptedDataDetail> encryptedDataDetails = (List<EncryptedDataDetail>) params[2];
        Object[] newParamsArr = Arrays.copyOf(params, params.length + 1);
        newParamsArr[newParamsArr.length - 1] =
            GitValidationParameters.builder()
                .gitConfig(config)
                .encryptedDataDetails(encryptedDataDetails)
                .isGitHostConnectivityCheck(featureFlagService.isEnabled(GIT_HOST_CONNECTIVITY, task.getAccountId()))
                .build();
        task.getData().setParameters(newParamsArr);
        return;
      default:
        noop();
    }
  }

  private List<String> ensureDelegateAvailableToExecuteTask(DelegateTask task) {
    if (task == null) {
      log.warn("Delegate task is null");
      throw new InvalidArgumentsException(Pair.of("args", "Delegate task is null"));
    }
    if (task.getAccountId() == null) {
      log.warn("Delegate task has null account ID");
      throw new InvalidArgumentsException(Pair.of("args", "Delegate task has null account ID"));
    }

    BatchDelegateSelectionLog batch = delegateSelectionLogsService.createBatch(task);

    List<String> activeDelegates = assignDelegateService.retrieveActiveDelegates(task.getAccountId(), batch);
    log.info("{} delegates {} are active", activeDelegates.size(), activeDelegates);

    List<String> eligibleDelegates = activeDelegates.stream()
                                         .filter(delegateId -> assignDelegateService.canAssign(batch, delegateId, task))
                                         .collect(toList());

    delegateSelectionLogsService.save(batch);

    if (activeDelegates.isEmpty()) {
      if (assignDelegateService.noInstalledDelegates(task.getAccountId())) {
        log.info("No installed delegates found for the account");
        alertService.openAlert(task.getAccountId(), GLOBAL_APP_ID, AlertType.NoInstalledDelegates,
            NoInstalledDelegatesAlert.builder().accountId(task.getAccountId()).build());
      } else {
        log.info("No delegates are active for the account");
        alertService.openAlert(task.getAccountId(), GLOBAL_APP_ID, AlertType.NoActiveDelegates,
            NoActiveDelegatesAlert.builder().accountId(task.getAccountId()).build());
      }
    } else if (eligibleDelegates.isEmpty()) {
      log.warn("{} delegates active but no delegates are eligible to execute task", activeDelegates.size());

      List<ExecutionCapability> selectorCapabilities = null;

      if (task.getExecutionCapabilities() != null) {
        selectorCapabilities =
            task.getExecutionCapabilities().stream().filter(c -> c instanceof SelectorCapability).collect(toList());
      } else {
        selectorCapabilities = emptyList();
      }

      String appId =
          task.getSetupAbstractions() == null ? null : task.getSetupAbstractions().get(Cd1SetupFields.APP_ID_FIELD);
      String envId =
          task.getSetupAbstractions() == null ? null : task.getSetupAbstractions().get(Cd1SetupFields.ENV_ID_FIELD);
      String infrastructureMappingId = task.getSetupAbstractions() == null
          ? null
          : task.getSetupAbstractions().get(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD);

      alertService.openAlert(task.getAccountId(), appId, NoEligibleDelegates,
          NoEligibleDelegatesAlert.builder()
              .accountId(task.getAccountId())
              .appId(appId)
              .envId(envId)
              .infraMappingId(infrastructureMappingId)
              .taskGroup(TaskType.valueOf(task.getData().getTaskType()).getTaskGroup())
              .taskType(TaskType.valueOf(task.getData().getTaskType()))
              .executionCapabilities(selectorCapabilities)
              .build());
    }

    log.info("{} delegates {} eligible to execute task", eligibleDelegates.size(), eligibleDelegates);
    return eligibleDelegates;
  }

  @Override
  public DelegateTaskPackage reportConnectionResults(
      String accountId, String delegateId, String taskId, List<DelegateConnectionResult> results) {
    assignDelegateService.saveConnectionResults(results);
    DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId, delegateId);
    if (delegateTask == null) {
      return null;
    }

    try (AutoLogContext ignore = new TaskLogContext(taskId, delegateTask.getData().getTaskType(),
             TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR)) {
      log.info("Delegate completed validating {} task", delegateTask.getData().isAsync() ? ASYNC : SYNC);

      UpdateOperations<DelegateTask> updateOperations =
          persistence.createUpdateOperations(DelegateTask.class)
              .addToSet(DelegateTaskKeys.validationCompleteDelegateIds, delegateId);
      Query<DelegateTask> updateQuery = persistence.createQuery(DelegateTask.class)
                                            .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                            .filter(DelegateTaskKeys.uuid, delegateTask.getUuid())
                                            .filter(DelegateTaskKeys.status, QUEUED)
                                            .field(DelegateTaskKeys.delegateId)
                                            .doesNotExist();
      persistence.update(updateQuery, updateOperations);

      long requiredDelegateCapabilities = 0;
      if (delegateTask.getExecutionCapabilities() != null) {
        requiredDelegateCapabilities = delegateTask.getExecutionCapabilities()
                                           .stream()
                                           .filter(e -> e.evaluationMode() == EvaluationMode.AGENT)
                                           .count();
      }

      // If all delegate task capabilities were evaluated and they were ok, we can assign the task
      if (requiredDelegateCapabilities == size(results)
          && results.stream().allMatch(DelegateConnectionResult::isValidated)) {
        return assignTask(delegateId, taskId, delegateTask);
      }
    }

    return null;
  }

  @Override
  public DelegateTaskPackage acquireDelegateTask(String accountId, String delegateId, String taskId) {
    try {
      Delegate delegate = delegateCache.get(accountId, delegateId, false);
      if (delegate == null || DelegateInstanceStatus.ENABLED != delegate.getStatus()) {
        log.warn("Delegate rejected to acquire task, because it was not found to be in {} status.",
            DelegateInstanceStatus.ENABLED);
        return null;
      }

      log.info("Acquiring delegate task");
      DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId, delegateId);
      if (delegateTask == null) {
        return null;
      }

      try (AutoLogContext ignore = new TaskLogContext(taskId, delegateTask.getData().getTaskType(),
               TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR)) {
        BatchDelegateSelectionLog batch = delegateSelectionLogsService.createBatch(delegateTask);
        boolean canAssign = assignDelegateService.canAssign(batch, delegateId, delegateTask);
        delegateSelectionLogsService.save(batch);

        if (!canAssign) {
          log.info("Delegate is not scoped for task");
          ensureDelegateAvailableToExecuteTask(delegateTask); // Raises an alert if there are no eligible delegates.
          return null;
        }

        if (delegateId != null && delegateId.equals(delegateTask.getMustExecuteOnDelegateId())) {
          return assignTask(delegateId, taskId, delegateTask);
        }

        if (featureFlagService.isEnabled(FeatureName.PER_AGENT_CAPABILITIES, accountId)) {
          return assignTask(delegateId, taskId, delegateTask);
        }

        if (assignDelegateService.shouldValidate(delegateTask, delegateId)) {
          setValidationStarted(delegateId, delegateTask);
          return resolvePreAssignmentExpressions(delegateTask, SecretManagerMode.APPLY);
        } else if (assignDelegateService.isWhitelisted(delegateTask, delegateId)) {
          return assignTask(delegateId, taskId, delegateTask);
        }

        log.info("Delegate is blacklisted for task");
        return null;
      }
    } finally {
      log.info("Done with acquire delegate task method");
    }
  }

  @Override
  public void failIfAllDelegatesFailed(String accountId, String delegateId, String taskId) {
    DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId, delegateId);
    if (delegateTask == null) {
      log.info("Task not found or was already assigned");
      return;
    }

    if (delegateTask.isForceExecute()) {
      log.info("Task is set for force execution");
      return;
    }

    try (AutoLogContext ignore = new TaskLogContext(taskId, delegateTask.getData().getTaskType(),
             TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR)) {
      if (!isValidationComplete(delegateTask)) {
        log.info("Task is still being validated");
        return;
      }
      // Check whether a whitelisted delegate is connected
      List<String> whitelistedDelegates = assignDelegateService.connectedWhitelistedDelegates(delegateTask);
      if (isNotEmpty(whitelistedDelegates)) {
        log.info("Waiting for task to be acquired by a whitelisted delegate: {}", whitelistedDelegates);
        return;
      }

      log.info("No connected whitelisted delegates found for task");
      String errorMessage = generateValidationError(delegateTask);
      log.info(errorMessage);
      DelegateResponseData response;
      if (delegateTask.getData().isAsync()) {
        response = ErrorNotifyResponseData.builder()
                       .failureTypes(EnumSet.of(FailureType.DELEGATE_PROVISIONING))
                       .errorMessage(errorMessage)
                       .build();
      } else {
        response =
            RemoteMethodReturnValueData.builder().exception(new InvalidRequestException(errorMessage, USER)).build();
      }
      delegateTaskService.processDelegateResponse(accountId, null, taskId,
          DelegateTaskResponse.builder().accountId(accountId).response(response).responseCode(ResponseCode.OK).build());
    }
  }

  private String generateValidationError(DelegateTask delegateTask) {
    String capabilities = "";
    List<ExecutionCapability> executionCapabilities = delegateTask.getExecutionCapabilities();
    if (isNotEmpty(executionCapabilities)) {
      capabilities = (executionCapabilities.size() > 4 ? executionCapabilities.subList(0, 4) : executionCapabilities)
                         .stream()
                         .map(ExecutionCapability::fetchCapabilityBasis)
                         .collect(joining(", "));
      if (executionCapabilities.size() > 4) {
        capabilities += ", and " + (executionCapabilities.size() - 4) + " more...";
      }
    }

    String delegates = null, timedoutDelegates = null;
    Set<String> validationCompleteDelegateIds = delegateTask.getValidationCompleteDelegateIds();
    Set<String> validatingDelegateIds = delegateTask.getValidatingDelegateIds();

    if (isNotEmpty(validationCompleteDelegateIds)) {
      delegates = join(", ",
          validationCompleteDelegateIds.stream()
              .map(delegateId -> {
                Delegate delegate = delegateCache.get(delegateTask.getAccountId(), delegateId, false);
                return delegate == null ? delegateId : delegate.getHostName();
              })
              .collect(toList()));
    } else {
      delegates = "no delegates";
    }

    if (isNotEmpty(validatingDelegateIds)) {
      timedoutDelegates = join(", ",
          validatingDelegateIds.stream()
              .filter(p -> !validationCompleteDelegateIds.contains(p))
              .map(delegateId -> {
                Delegate delegate = delegateCache.get(delegateTask.getAccountId(), delegateId, false);
                return delegate == null ? delegateId : delegate.getHostName();
              })
              .collect(joining()));
    } else {
      timedoutDelegates = "no delegates timedout";
    }

    return format("No eligible delegates could perform the required capabilities for this task: [ %s ]%n"
            + "  -  The capabilities were tested by the following delegates: [ %s ]%n"
            + "  -  Following delegates were validating but never returned: [ %s ]%n"
            + "  -  Other delegates (if any) may have been offline or were not eligible due to tag or scope restrictions.",
        capabilities, delegates, timedoutDelegates);
  }

  @VisibleForTesting
  void setValidationStarted(String delegateId, DelegateTask delegateTask) {
    log.info("Delegate to validate {} task", delegateTask.getData().isAsync() ? ASYNC : SYNC);
    UpdateOperations<DelegateTask> updateOperations = persistence.createUpdateOperations(DelegateTask.class)
                                                          .addToSet(DelegateTaskKeys.validatingDelegateIds, delegateId);
    Query<DelegateTask> updateQuery = persistence.createQuery(DelegateTask.class)
                                          .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                          .filter(DelegateTaskKeys.uuid, delegateTask.getUuid())
                                          .filter(DelegateTaskKeys.status, QUEUED)
                                          .field(DelegateTaskKeys.delegateId)
                                          .doesNotExist();
    persistence.update(updateQuery, updateOperations);

    persistence.update(updateQuery.field(DelegateTaskKeys.validationStartedAt).doesNotExist(),
        persistence.createUpdateOperations(DelegateTask.class)
            .set(DelegateTaskKeys.validationStartedAt, clock.millis()));
  }

  private boolean isValidationComplete(DelegateTask delegateTask) {
    Set<String> validatingDelegates = delegateTask.getValidatingDelegateIds();
    Set<String> completeDelegates = delegateTask.getValidationCompleteDelegateIds();
    boolean allDelegatesFinished = isNotEmpty(validatingDelegates) && isNotEmpty(completeDelegates)
        && completeDelegates.containsAll(validatingDelegates);
    if (allDelegatesFinished) {
      log.info("Validation attempts are complete for task", delegateTask.getUuid());
    }
    boolean validationTimedOut = delegateTask.getValidationStartedAt() != null
        && clock.millis() - delegateTask.getValidationStartedAt() > VALIDATION_TIMEOUT;
    if (validationTimedOut) {
      log.info("Validation timed out for task", delegateTask.getUuid());
    }
    return allDelegatesFinished || validationTimedOut;
  }

  private void clearFromValidationCache(DelegateTask delegateTask) {
    UpdateOperations<DelegateTask> updateOperations = persistence.createUpdateOperations(DelegateTask.class)
                                                          .unset(DelegateTaskKeys.validatingDelegateIds)
                                                          .unset(DelegateTaskKeys.validationCompleteDelegateIds);
    Query<DelegateTask> updateQuery = persistence.createQuery(DelegateTask.class)
                                          .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                          .filter(DelegateTaskKeys.uuid, delegateTask.getUuid())
                                          .filter(DelegateTaskKeys.status, QUEUED)
                                          .field(DelegateTaskKeys.delegateId)
                                          .doesNotExist();
    persistence.update(updateQuery, updateOperations);
  }

  @VisibleForTesting
  DelegateTask getUnassignedDelegateTask(String accountId, String taskId, String delegateId) {
    DelegateTask delegateTask = persistence.createQuery(DelegateTask.class)
                                    .filter(DelegateTaskKeys.accountId, accountId)
                                    .filter(DelegateTaskKeys.uuid, taskId)
                                    .get();
    if (delegateTask != null) {
      try (AutoLogContext ignore = new TaskLogContext(taskId, delegateTask.getData().getTaskType(),
               TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR)) {
        if (delegateTask.getDelegateId() == null && delegateTask.getStatus() == QUEUED) {
          log.info("Found unassigned delegate task");
          return delegateTask;
        } else if (delegateId.equals(delegateTask.getDelegateId())) {
          log.info("Returning already assigned task to delegate from getUnassigned");
          return delegateTask;
        }
        log.info("Task not available for delegate - it was assigned to {} and has status {}",
            delegateTask.getDelegateId(), delegateTask.getStatus());
      }
    } else {
      log.info("Task no longer exists", taskId);
    }
    return null;
  }

  private DelegateTaskPackage resolvePreAssignmentExpressions(DelegateTask delegateTask, SecretManagerMode mode) {
    try {
      ManagerPreExecutionExpressionEvaluator managerPreExecutionExpressionEvaluator =
          new ManagerPreExecutionExpressionEvaluator(mode, serviceTemplateService, configService,
              artifactCollectionUtils, featureFlagService, managerDecryptionService, secretManager,
              delegateTask.getAccountId(), delegateTask.getWorkflowExecutionId(),
              delegateTask.getData().getExpressionFunctorToken(), ngSecretService, delegateTask.getSetupAbstractions());

      List<ExecutionCapability> executionCapabilityList = emptyList();
      if (isNotEmpty(delegateTask.getExecutionCapabilities())) {
        executionCapabilityList = delegateTask.getExecutionCapabilities()
                                      .stream()
                                      .filter(x -> x.evaluationMode() == EvaluationMode.AGENT)
                                      .collect(toList());
      }

      DelegateTaskPackageBuilder delegateTaskPackageBuilder = DelegateTaskPackage.builder()
                                                                  .accountId(delegateTask.getAccountId())
                                                                  .delegateId(delegateTask.getDelegateId())
                                                                  .delegateTaskId(delegateTask.getUuid())
                                                                  .data(delegateTask.getData())
                                                                  .executionCapabilities(executionCapabilityList)
                                                                  .delegateCallbackToken(delegateTask.getDriverId());

      if (featureFlagService.isEnabled(FeatureName.LOG_STREAMING_INTEGRATION, delegateTask.getAccountId())) {
        try {
          String logStreamingAccountToken = logStreamingAccountTokenCache.get(delegateTask.getAccountId());

          if (isNotBlank(logStreamingAccountToken)) {
            delegateTaskPackageBuilder.logStreamingToken(logStreamingAccountToken);
          }
        } catch (ExecutionException e) {
          log.warn("Unable to retrieve the log streaming service account token, while preparing delegate task package");
          throw new InvalidRequestException(e.getMessage() + "\nPlease ensure log service is running.", e);
        }

        delegateTaskPackageBuilder.logStreamingAbstractions(delegateTask.getLogStreamingAbstractions());
      }

      if (delegateTask.getData().getParameters() == null || delegateTask.getData().getParameters().length != 1
          || !(delegateTask.getData().getParameters()[0] instanceof TaskParameters)) {
        return delegateTaskPackageBuilder.build();
      }

      NgSecretManagerFunctor ngSecretManagerFunctor =
          (NgSecretManagerFunctor) managerPreExecutionExpressionEvaluator.getNgSecretManagerFunctor();

      SecretManagerFunctor secretManagerFunctor =
          (SecretManagerFunctor) managerPreExecutionExpressionEvaluator.getSecretManagerFunctor();

      SweepingOutputSecretFunctor sweepingOutputSecretFunctor =
          managerPreExecutionExpressionEvaluator.getSweepingOutputSecretFunctor();

      ExpressionReflectionUtils.applyExpression(delegateTask.getData().getParameters()[0], (secretMode, value) -> {
        if (value == null) {
          return null;
        }
        return managerPreExecutionExpressionEvaluator.substitute(value, new HashMap<>());
        // TODO: this code is causing the second issue in DEL-1167
        //        if (secretManagerFunctor != null && secretMode == DISALLOW_SECRETS
        //            && secretManagerFunctor.getEvaluatedSecrets().size() > 0) {
        //          throw new InvalidRequestException(format("Expression %s is not allowed to have secrets.",
        //          substituted));
        //        }
        //        return mode == CHECK_FOR_SECRETS ? value : substituted;
      });

      if (secretManagerFunctor == null && ngSecretManagerFunctor == null) {
        return null;
      }

      addSecretManagerFunctorConfigs(
          delegateTaskPackageBuilder, secretManagerFunctor, ngSecretManagerFunctor, sweepingOutputSecretFunctor);

      return delegateTaskPackageBuilder.build();
    } catch (CriticalExpressionEvaluationException exception) {
      log.error("Exception in ManagerPreExecutionExpressionEvaluator ", exception);
      Query<DelegateTask> taskQuery = persistence.createQuery(DelegateTask.class)
                                          .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                          .filter(DelegateTaskKeys.uuid, delegateTask.getUuid());
      DelegateTaskResponse response =
          DelegateTaskResponse.builder()
              .response(ErrorNotifyResponseData.builder().errorMessage(ExceptionUtils.getMessage(exception)).build())
              .responseCode(ResponseCode.FAILED)
              .accountId(delegateTask.getAccountId())
              .build();
      delegateTaskService.handleResponse(delegateTask, taskQuery, response);
      return null;
    }
  }

  private void addSecretManagerFunctorConfigs(DelegateTaskPackageBuilder delegateTaskPackageBuilder,
      SecretManagerFunctor secretManagerFunctor, NgSecretManagerFunctor ngSecretManagerFunctor,
      SweepingOutputSecretFunctor sweepingOutputSecretFunctor) {
    Map<String, EncryptionConfig> encryptionConfigs = new HashMap<>();
    Map<String, SecretDetail> secretDetails = new HashMap<>();
    Set<String> secrets = new HashSet<>();

    if (secretManagerFunctor != null) {
      encryptionConfigs.putAll(secretManagerFunctor.getEncryptionConfigs());
      secretDetails.putAll(secretManagerFunctor.getSecretDetails());
      if (isNotEmpty(secretManagerFunctor.getEvaluatedSecrets())) {
        secrets.addAll(secretManagerFunctor.getEvaluatedSecrets().values());
      }
    }

    if (ngSecretManagerFunctor != null) {
      encryptionConfigs.putAll(ngSecretManagerFunctor.getEncryptionConfigs());
      secretDetails.putAll(ngSecretManagerFunctor.getSecretDetails());
      if (isNotEmpty(ngSecretManagerFunctor.getEvaluatedSecrets())) {
        secrets.addAll(ngSecretManagerFunctor.getEvaluatedSecrets().values());
      }
    }

    if (sweepingOutputSecretFunctor != null) {
      if (isNotEmpty(sweepingOutputSecretFunctor.getEvaluatedSecrets())) {
        secrets.addAll(sweepingOutputSecretFunctor.getEvaluatedSecrets());
      }
    }

    delegateTaskPackageBuilder.encryptionConfigs(encryptionConfigs);
    delegateTaskPackageBuilder.secretDetails(secretDetails);
    delegateTaskPackageBuilder.secrets(secrets);
  }

  private DelegateTaskPackage getDelegatePackageWithEncryptionConfig(DelegateTask delegateTask) {
    try {
      if (CapabilityHelper.isTaskParameterType(delegateTask.getData())) {
        return resolvePreAssignmentExpressions(delegateTask, SecretManagerMode.DRY_RUN);
      } else {
        // TODO: Ideally we should not land here, as we should always be passing TaskParameter only for
        // TODO: delegate task. But for now, this is needed. (e.g. Tasks containing Jenkinsonfig, BambooConfig etc.)
        Map<String, EncryptionConfig> encryptionConfigMap =
            CapabilityHelper.fetchEncryptionDetailsListFromParameters(delegateTask.getData());

        return DelegateTaskPackage.builder()
            .accountId(delegateTask.getAccountId())
            .delegateId(delegateTask.getDelegateId())
            .delegateTaskId(delegateTask.getUuid())
            .data(delegateTask.getData())
            .encryptionConfigs(encryptionConfigMap)
            .build();
      }
    } catch (CriticalExpressionEvaluationException exception) {
      log.error("Exception in ManagerPreExecutionExpressionEvaluator ", exception);
      Query<DelegateTask> taskQuery = persistence.createQuery(DelegateTask.class)
                                          .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                          .filter(DelegateTaskKeys.uuid, delegateTask.getUuid());
      DelegateTaskResponse response =
          DelegateTaskResponse.builder()
              .response(ErrorNotifyResponseData.builder().errorMessage(ExceptionUtils.getMessage(exception)).build())
              .responseCode(ResponseCode.FAILED)
              .accountId(delegateTask.getAccountId())
              .build();
      delegateTaskService.handleResponse(delegateTask, taskQuery, response);
      return null;
    }
  }

  @Override
  public void publishTaskProgressResponse(
      String accountId, String driverId, String delegateTaskId, DelegateProgressData responseData) {
    DelegateCallbackService delegateCallbackService = delegateCallbackRegistry.obtainDelegateCallbackService(driverId);
    if (delegateCallbackService == null) {
      return;
    }
    delegateCallbackService.publishTaskProgressResponse(
        delegateTaskId, generateUuid(), kryoSerializer.asDeflatedBytes(responseData));
  }

  @VisibleForTesting
  DelegateTaskPackage assignTask(String delegateId, String taskId, DelegateTask delegateTask) {
    // Clear pending validations. No longer need to track since we're assigning.
    clearFromValidationCache(delegateTask);

    log.info("Assigning {} task to delegate", delegateTask.getData().isAsync() ? ASYNC : SYNC);
    Query<DelegateTask> query = persistence.createQuery(DelegateTask.class)
                                    .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                    .filter(DelegateTaskKeys.uuid, taskId)
                                    .filter(DelegateTaskKeys.status, QUEUED)
                                    .field(DelegateTaskKeys.delegateId)
                                    .doesNotExist()
                                    .project(DelegateTaskKeys.data_parameters, false);
    UpdateOperations<DelegateTask> updateOperations =
        persistence.createUpdateOperations(DelegateTask.class)
            .set(DelegateTaskKeys.delegateId, delegateId)
            .set(DelegateTaskKeys.status, STARTED)
            .set(DelegateTaskKeys.expiry, currentTimeMillis() + delegateTask.getData().getTimeout());
    DelegateTask task = persistence.findAndModifySystemData(query, updateOperations, HPersistence.returnNewOptions);
    // If the task wasn't updated because delegateId already exists then query for the task with the delegateId in
    // case client is retrying the request
    if (task != null) {
      try (
          DelayLogContext ignore = new DelayLogContext(task.getLastUpdatedAt() - task.getCreatedAt(), OVERRIDE_ERROR)) {
        log.info("Task assigned to delegate");
      }
      task.getData().setParameters(delegateTask.getData().getParameters());

      BatchDelegateSelectionLog batch = delegateSelectionLogsService.createBatch(delegateTask);
      delegateSelectionLogsService.logTaskAssigned(batch, task.getAccountId(), delegateId);
      delegateSelectionLogsService.save(batch);

      delegateTaskStatusObserverSubject.fireInform(DelegateTaskStatusObxxxxxxxx:onTaskAssigned,
          delegateTask.getAccountId(), taskId, delegateId, task.getData().getTimeout());

      return resolvePreAssignmentExpressions(task, SecretManagerMode.APPLY);
    }
    task = persistence.createQuery(DelegateTask.class)
               .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
               .filter(DelegateTaskKeys.uuid, taskId)
               .filter(DelegateTaskKeys.status, STARTED)
               .filter(DelegateTaskKeys.delegateId, delegateId)
               .project(DelegateTaskKeys.data_parameters, false)
               .get();
    if (task == null) {
      log.info("Task no longer available for delegate");
      return null;
    }

    task.getData().setParameters(delegateTask.getData().getParameters());
    log.info("Returning previously assigned task to delegate");
    return resolvePreAssignmentExpressions(task, SecretManagerMode.APPLY);
  }

  @Override
  public boolean filter(String delegateId, DelegateTaskAbortEvent taskAbortEvent) {
    return persistence.createQuery(DelegateTask.class)
               .filter(DelegateTaskKeys.accountId, taskAbortEvent.getAccountId())
               .filter(DelegateTaskKeys.uuid, taskAbortEvent.getDelegateTaskId())
               .filter(DelegateTaskKeys.delegateId, delegateId)
               .getKey()
        != null;
  }

  @Override
  public String expireTask(String accountId, String delegateTaskId) {
    String errorMessage = null;
    try (AutoLogContext ignore1 = new TaskLogContext(delegateTaskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      if (delegateTaskId == null) {
        log.warn("Delegate task id was null", new IllegalArgumentException());
        return errorMessage;
      }
      log.info("Expiring delegate task");
      Query<DelegateTask> delegateTaskQuery = getRunningTaskQuery(accountId, delegateTaskId);

      DelegateTask delegateTask = delegateTaskQuery.get();
      if (delegateTask != null) {
        try (AutoLogContext ignore3 = new TaskLogContext(delegateTaskId, delegateTask.getData().getTaskType(),
                 TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR)) {
          errorMessage =
              "Task expired. " + assignDelegateService.getActiveDelegateAssignmentErrorMessage(EXPIRED, delegateTask);
          log.info("Marking task as expired: {}", errorMessage);

          if (isNotBlank(delegateTask.getWaitId())) {
            waitNotifyEngine.doneWith(
                delegateTask.getWaitId(), ErrorNotifyResponseData.builder().errorMessage(errorMessage).build());
          }
        }
      }

      endTask(accountId, delegateTaskId, delegateTaskQuery, ERROR);
    }
    return errorMessage;
  }

  @Override
  public DelegateTask abortTask(String accountId, String delegateTaskId) {
    try (AutoLogContext ignore1 = new TaskLogContext(delegateTaskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      if (delegateTaskId == null) {
        log.warn("Delegate task id was null", new IllegalArgumentException());
        return null;
      }
      log.info("Aborting delegate task");

      persistence.save(DelegateSyncTaskResponse.builder()
                           .uuid(delegateTaskId)
                           .responseData(kryoSerializer.asDeflatedBytes(
                               ErrorNotifyResponseData.builder().errorMessage("Delegate task was aborted").build()))
                           .build());

      return endTask(accountId, delegateTaskId, getRunningTaskQuery(accountId, delegateTaskId), ABORTED);
    }
  }

  private DelegateTask endTask(
      String accountId, String delegateTaskId, Query<DelegateTask> delegateTaskQuery, DelegateTask.Status status) {
    UpdateOperations updateOperations =
        persistence.createUpdateOperations(DelegateTask.class).set(DelegateTaskKeys.status, status);

    DelegateTask oldTask =
        persistence.findAndModify(delegateTaskQuery, updateOperations, HPersistence.returnOldOptions);

    broadcasterFactory.lookup(STREAM_DELEGATE + accountId, true)
        .broadcast(aDelegateTaskAbortEvent().withAccountId(accountId).withDelegateTaskId(delegateTaskId).build());

    return oldTask;
  }

  private Query<DelegateTask> getRunningTaskQuery(String accountId, String delegateTaskId) {
    return persistence.createQuery(DelegateTask.class)
        .filter(DelegateTaskKeys.uuid, delegateTaskId)
        .filter(DelegateTaskKeys.accountId, accountId)
        .filter(DelegateTaskKeys.data_async, Boolean.TRUE)
        .field(DelegateTaskKeys.status)
        .in(runningStatuses());
  }

  @Override
  public List<DelegateTaskEvent> getDelegateTaskEvents(String accountId, String delegateId, boolean syncOnly) {
    List<DelegateTaskEvent> delegateTaskEvents = new ArrayList<>(getQueuedEvents(accountId, delegateId, true));
    if (!syncOnly) {
      delegateTaskEvents.addAll(getQueuedEvents(accountId, delegateId, false));
      delegateTaskEvents.addAll(getAbortedEvents(accountId, delegateId));
    }

    log.info("Dispatched delegateTaskIds: {}",
        join(",", delegateTaskEvents.stream().map(DelegateTaskEvent::getDelegateTaskId).collect(toList())));

    return delegateTaskEvents;
  }

  private List<DelegateTaskEvent> getQueuedEvents(String accountId, String delegateId, boolean sync) {
    // TODO - add assignment filter here (scopes. selectors, ...)
    Query<DelegateTask> delegateTaskQuery =
        persistence.createQuery(DelegateTask.class)
            .filter(DelegateTaskKeys.accountId, accountId)
            .filter(DelegateTaskKeys.version, versionInfoManager.getVersionInfo().getVersion())
            .filter(DelegateTaskKeys.status, QUEUED)
            .filter(DelegateTaskKeys.data_async, !sync)
            .field(DelegateTaskKeys.delegateId)
            .doesNotExist()
            .field(DelegateTaskKeys.expiry)
            .greaterThan(currentTimeMillis());

    if (featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, accountId)) {
      delegateTaskQuery.filter(DelegateTaskKeys.preAssignedDelegateId, delegateId);
    }

    return delegateTaskQuery.asKeyList()
        .stream()
        .map(taskKey
            -> aDelegateTaskEvent()
                   .withAccountId(accountId)
                   .withDelegateTaskId(taskKey.getId().toString())
                   .withSync(sync)
                   .build())
        .collect(toList());
  }

  private List<DelegateTaskEvent> getAbortedEvents(String accountId, String delegateId) {
    Query<DelegateTask> abortedQuery = persistence.createQuery(DelegateTask.class)
                                           .filter(DelegateTaskKeys.accountId, accountId)
                                           .filter(DelegateTaskKeys.status, ABORTED)
                                           .filter(DelegateTaskKeys.data_async, Boolean.TRUE)
                                           .filter(DelegateTaskKeys.delegateId, delegateId);

    // Send abort event only once by clearing delegateId
    persistence.update(
        abortedQuery, persistence.createUpdateOperations(DelegateTask.class).unset(DelegateTaskKeys.delegateId));

    return abortedQuery.project(DelegateTaskKeys.accountId, true)
        .asList()
        .stream()
        .map(delegateTask
            -> aDelegateTaskAbortEvent()
                   .withAccountId(delegateTask.getAccountId())
                   .withDelegateTaskId(delegateTask.getUuid())
                   .withSync(false)
                   .build())
        .collect(toList());
  }

  private String getVersion() {
    return versionInfoManager.getVersionInfo().getVersion();
  }

  @Override
  public void deleteByAccountId(String accountId) {
    persistence.delete(persistence.createQuery(Delegate.class).filter(DelegateKeys.accountId, accountId));
  }

  @Override
  public Optional<DelegateTask> fetchDelegateTask(String accountId, String taskId) {
    return Optional.ofNullable(persistence.createQuery(DelegateTask.class)
                                   .filter(DelegateTaskKeys.accountId, accountId)
                                   .filter(DelegateTaskKeys.uuid, taskId)
                                   .get());
  }

  @VisibleForTesting
  protected String retrieveLogStreamingAccountToken(String accountId) throws IOException {
    return SafeHttpCall.executeWithExceptions(logStreamingServiceRestClient.retrieveAccountToken(
        mainConfiguration.getLogStreamingServiceConfig().getServiceToken(), accountId));
  }
}
