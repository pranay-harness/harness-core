package software.wings.service.impl;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Sets.newHashSet;
import static freemarker.template.Configuration.VERSION_2_3_23;
import static io.harness.beans.DelegateTask.Status.ABORTED;
import static io.harness.beans.DelegateTask.Status.ERROR;
import static io.harness.beans.DelegateTask.Status.FINISHED;
import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.beans.DelegateTask.Status.STARTED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.eraro.ErrorCode.UNAVAILABLE_DELEGATES;
import static io.harness.exception.WingsException.NOBODY;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.DelegateConnection.defaultExpiryTimeInMinutes;
import static software.wings.beans.DelegateSequenceConfig.Builder.aDelegateSequenceBuilder;
import static software.wings.beans.DelegateTaskAbortEvent.Builder.aDelegateTaskAbortEvent;
import static software.wings.beans.DelegateTaskEvent.DelegateTaskEventBuilder.aDelegateTaskEvent;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.FeatureName.DELEGATE_CAPABILITY_FRAMEWORK;
import static software.wings.beans.ServiceSecretKey.ServiceType.EVENT_SERVICE;
import static software.wings.beans.ServiceSecretKey.ServiceType.LEARNING_ENGINE;
import static software.wings.beans.TaskType.HOST_VALIDATION;
import static software.wings.beans.alert.AlertType.NoEligibleDelegates;
import static software.wings.common.Constants.DELEGATE_DIR;
import static software.wings.common.Constants.DOCKER_DELEGATE;
import static software.wings.common.Constants.ECS_DELEGATE;
import static software.wings.common.Constants.KUBERNETES_DELEGATE;
import static software.wings.common.Constants.MAX_DELEGATE_LAST_HEARTBEAT;
import static software.wings.common.Constants.SELF_DESTRUCT;
import static software.wings.delegatetasks.RemoteMethodReturnValueData.Builder.aRemoteMethodReturnValueData;
import static software.wings.utils.KubernetesConvention.getAccountIdentifier;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.github.zafarkhaja.semver.Version;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoGridFSException;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.CapabilityUtils;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.CriticalExpressionEvaluationException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionReflectionUtils;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.network.Http;
import io.harness.persistence.HPersistence;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.serializer.KryoUtils;
import io.harness.stream.BoundedInputStream;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.ErrorNotifyResponseData;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request.Builder;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.atmosphere.cpr.BroadcasterFactory;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.app.DeployMode;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateConnection.DelegateConnectionKeys;
import software.wings.beans.DelegateConnectionHeartbeat;
import software.wings.beans.DelegatePackage;
import software.wings.beans.DelegateProfile;
import software.wings.beans.DelegateProfileParams;
import software.wings.beans.DelegateSequenceConfig;
import software.wings.beans.DelegateSequenceConfig.DelegateSequenceConfigKeys;
import software.wings.beans.DelegateStatus;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.DelegateTaskResponse.ResponseCode;
import software.wings.beans.Event.Type;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.FileMetadata;
import software.wings.beans.HostValidationTaskParameters;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DelegateProfileErrorAlert;
import software.wings.beans.alert.DelegatesDownAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.beans.alert.NoEligibleDelegatesAlert;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerPreExecutionExpressionEvaluator;
import software.wings.expression.SecretFunctor;
import software.wings.expression.SecretManagerFunctor;
import software.wings.features.DelegatesFeature;
import software.wings.features.api.UsageLimitedFeature;
import software.wings.licensing.LicenseService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.impl.infra.InfraDownloadService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.Misc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.NotFoundException;

@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateServiceImpl implements DelegateService, Runnable {
  private static final Configuration cfg = new Configuration(VERSION_2_3_23);
  private static final int MAX_DELEGATE_META_INFO_ENTRIES = 10000;
  private static final Set<DelegateTask.Status> TASK_COMPLETED_STATUSES = ImmutableSet.of(FINISHED, ABORTED, ERROR);
  private static final String HARNESS_ECS_DELEGATE = "Harness-ECS-Delegate";
  private static final String DELIMITER = "_";
  public static final String ECS = "ECS";

  public static final String HARNESS_DELEGATE_VALUES_YAML = "harness-delegate-values";

  static {
    cfg.setTemplateLoader(new ClassTemplateLoader(DelegateServiceImpl.class, "/delegatetemplates"));
  }

  public static final long VALIDATION_TIMEOUT = TimeUnit.SECONDS.toMillis(12);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private AccountService accountService;
  @Inject private LicenseService licenseService;
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
  @Inject private DelegateProfileService delegateProfileService;
  @Inject private LearningEngineService learningEngineService;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private SecretManager secretManager;
  @Inject private ExpressionEvaluator evaluator;
  @Inject private FileService fileService;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private ConfigService configService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private PersistentLocker persistentLocker;
  @Inject @Named(DelegatesFeature.FEATURE_NAME) private UsageLimitedFeature delegatesFeature;

  private final Map<String, Object> syncTaskWaitMap = new ConcurrentHashMap<>();

  private LoadingCache<String, String> delegateVersionCache = CacheBuilder.newBuilder()
                                                                  .maximumSize(10000)
                                                                  .expireAfterWrite(1, TimeUnit.MINUTES)
                                                                  .build(new CacheLoader<String, String>() {
                                                                    @Override
                                                                    public String load(String accountId) {
                                                                      return fetchDelegateMetadataFromStorage();
                                                                    }
                                                                  });

  private LoadingCache<String, Optional<Delegate>> delegateCache =
      CacheBuilder.newBuilder()
          .maximumSize(MAX_DELEGATE_META_INFO_ENTRIES)
          .expireAfterWrite(1, TimeUnit.MINUTES)
          .build(new CacheLoader<String, Optional<Delegate>>() {
            @Override
            public Optional<Delegate> load(String delegateId) throws NotFoundException {
              return Optional.ofNullable(
                  wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.uuid, delegateId).get());
            }
          });

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  @SuppressWarnings("PMD")
  public void run() {
    try {
      if (isNotEmpty(syncTaskWaitMap)) {
        List<String> completedSyncTasks = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                              .filter(DelegateTaskKeys.async, false)
                                              .field(DelegateTaskKeys.status)
                                              .in(TASK_COMPLETED_STATUSES)
                                              .field(DelegateTaskKeys.uuid)
                                              .in(syncTaskWaitMap.keySet())
                                              .asKeyList()
                                              .stream()
                                              .map(key -> key.getId().toString())
                                              .collect(toList());
        for (String taskId : completedSyncTasks) {
          if (syncTaskWaitMap.get(taskId) != null) {
            synchronized (syncTaskWaitMap.get(taskId)) {
              syncTaskWaitMap.get(taskId).notifyAll();
            }
          }
        }
      }
    } catch (Throwable exception) {
      logger.error("Exception happened in run.", exception);
      if (exception instanceof Exception) {
        logger.warn("Exception is type of Exception. Ignoring.");
      } else {
        // Error class. Let it propagate.
        throw exception;
      }
    }
  }

  @Override
  public PageResponse<Delegate> list(PageRequest<Delegate> pageRequest) {
    return wingsPersistence.query(Delegate.class, pageRequest);
  }

  @Override
  public List<String> getKubernetesDelegateNames(String accountId) {
    return wingsPersistence.createQuery(Delegate.class)
        .filter(DelegateKeys.accountId, accountId)
        .field(DelegateKeys.delegateName)
        .exists()
        .project(DelegateKeys.delegateName, true)
        .asList()
        .stream()
        .map(Delegate::getDelegateName)
        .distinct()
        .sorted(naturalOrder())
        .collect(toList());
  }

  @Override
  public Set<String> getAllDelegateTags(String accountId) {
    List<Delegate> delegates = wingsPersistence.createQuery(Delegate.class)
                                   .filter(DelegateKeys.accountId, accountId)
                                   .field(DelegateKeys.tags)
                                   .exists()
                                   .asList();
    if (isNotEmpty(delegates)) {
      Set<String> tags = newHashSet();
      delegates.forEach(delegate -> {
        if (isNotEmpty(delegate.getTags())) {
          tags.addAll(delegate.getTags());
        }
      });
      return tags;
    }
    return emptySet();
  }

  @Override
  public List<String> getAvailableVersions(String accountId) {
    DelegateStatus status = getDelegateStatus(accountId);
    return status.getPublishedVersions();
  }

  @Override
  public DelegateStatus getDelegateStatus(String accountId) {
    DelegateConfiguration delegateConfiguration = accountService.getDelegateConfiguration(accountId);
    List<Delegate> delegates =
        wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.accountId, accountId).asList();
    List<DelegateConnection> delegateConnections = wingsPersistence.createQuery(DelegateConnection.class)
                                                       .filter(DelegateConnectionKeys.accountId, accountId)
                                                       .project(DelegateConnectionKeys.delegateId, true)
                                                       .project(DelegateConnectionKeys.version, true)
                                                       .project(DelegateConnectionKeys.lastHeartbeat, true)
                                                       .asList();

    return DelegateStatus.builder()
        .publishedVersions(delegateConfiguration.getDelegateVersions())
        .delegates(delegates.stream()
                       .map(delegate
                           -> DelegateStatus.DelegateInner.builder()
                                  .uuid(delegate.getUuid())
                                  .delegateName(delegate.getDelegateName())
                                  .description(delegate.getDescription())
                                  .hostName(delegate.getHostName())
                                  .delegateGroupName(delegate.getDelegateGroupName())
                                  .ip(delegate.getIp())
                                  .status(delegate.getStatus())
                                  .lastHeartBeat(delegate.getLastHeartBeat())
                                  .delegateProfileId(delegate.getDelegateProfileId())
                                  .excludeScopes(delegate.getExcludeScopes())
                                  .includeScopes(delegate.getIncludeScopes())
                                  .tags(delegate.getTags())
                                  .profileExecutedAt(delegate.getProfileExecutedAt())
                                  .profileError(delegate.isProfileError())
                                  .sampleDelegate(delegate.isSampleDelegate())
                                  .connections(delegateConnections.stream()
                                                   .filter(delegateConnection
                                                       -> StringUtils.equals(
                                                           delegateConnection.getDelegateId(), delegate.getUuid()))
                                                   .map(delegateConnection
                                                       -> DelegateStatus.DelegateInner.DelegateConnectionInner.builder()
                                                              .uuid(delegateConnection.getUuid())
                                                              .lastHeartbeat(delegateConnection.getLastHeartbeat())
                                                              .version(delegateConnection.getVersion())
                                                              .build())
                                                   .collect(Collectors.toList()))
                                  .build())
                       .collect(Collectors.toList()))
        .build();
  }

  @Override
  public Delegate get(String accountId, String delegateId, boolean forceRefresh) {
    try {
      if (forceRefresh) {
        delegateCache.refresh(delegateId);
      }
      return delegateCache.get(delegateId).orElse(null);
    } catch (ExecutionException e) {
      logger.error("Execution exception", e);
    } catch (UncheckedExecutionException e) {
      logger.error("Delegate not found exception", e);
    }
    return null;
  }

  @Override
  public Delegate update(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = getDelegateUpdateOperations(delegate);

    if (ECS.equals(delegate.getDelegateType())) {
      return updateEcsDelegate(delegate, true);
    } else {
      logger.info("Updating delegate : {}", delegate.getUuid());
      return updateDelegate(delegate, updateOperations);
    }
  }

  private Delegate updateEcsDelegate(Delegate delegate, boolean updateEntireEcsCluster) {
    UpdateOperations<Delegate> updateOperations = getDelegateUpdateOperations(delegate);
    if (updateEntireEcsCluster) {
      return updateAllDelegatesIfECSType(delegate, updateOperations, "ALL");
    } else {
      logger.info("Updating delegate : {}", delegate.getUuid());
      if (isDelegateWithoutPollingEnabled(delegate)) {
        // This updates delegates, as well as delegateConnection and taksBeingExecuted on delegate
        return updateDelegate(delegate, updateOperations);
      } else {
        // only update lastHeartbeatAt
        return updateHeartbeatForDelegateWithPollingEnabled(delegate);
      }
    }
  }

  private UpdateOperations<Delegate> getDelegateUpdateOperations(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = wingsPersistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, DelegateKeys.ip, delegate.getIp());
    setUnset(updateOperations, DelegateKeys.status, delegate.getStatus());
    setUnset(updateOperations, DelegateKeys.lastHeartBeat, delegate.getLastHeartBeat());
    setUnset(updateOperations, DelegateKeys.connected, delegate.isConnected());
    setUnset(updateOperations, DelegateKeys.version, delegate.getVersion());
    setUnset(updateOperations, DelegateKeys.description, delegate.getDescription());
    setUnset(updateOperations, DelegateKeys.delegateProfileId, delegate.getDelegateProfileId());
    setUnset(updateOperations, DelegateKeys.sampleDelegate, delegate.isSampleDelegate());
    return updateOperations;
  }
  @Override
  public Delegate updateDescription(String accountId, String delegateId, String newDescription) {
    logger.info("Updating delegate : {} with new description", delegateId);
    wingsPersistence.update(wingsPersistence.createQuery(Delegate.class)
                                .filter(DelegateKeys.accountId, accountId)
                                .filter(DelegateKeys.uuid, delegateId),
        wingsPersistence.createUpdateOperations(Delegate.class).set(DelegateKeys.description, newDescription));

    return get(accountId, delegateId, true);
  }

  @Override
  public Delegate updateHeartbeatForDelegateWithPollingEnabled(Delegate delegate) {
    wingsPersistence.update(wingsPersistence.createQuery(Delegate.class)
                                .filter(DelegateKeys.accountId, delegate.getAccountId())
                                .filter(DelegateKeys.uuid, delegate.getUuid()),
        wingsPersistence.createUpdateOperations(Delegate.class)
            .set(DelegateKeys.lastHeartBeat, System.currentTimeMillis())
            .set(DelegateKeys.connected, true));
    touchExecutingTasks(delegate);

    Delegate existingDelegate = get(delegate.getAccountId(), delegate.getUuid(), false);

    if (licenseService.isAccountDeleted(existingDelegate.getAccountId())) {
      existingDelegate.setStatus(Status.DELETED);
    }

    return existingDelegate;
  }

  @Override
  public Delegate updateTags(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = wingsPersistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, DelegateKeys.tags, delegate.getTags());
    logger.info("Updating delegate tags : Delegate:{} tags:{}", delegate.getUuid(), delegate.getTags());

    if (ECS.equals(delegate.getDelegateType())) {
      return updateAllDelegatesIfECSType(delegate, updateOperations, "TAGS");
    } else {
      Delegate updatedDelegate = updateDelegate(delegate, updateOperations);
      if (System.currentTimeMillis() - updatedDelegate.getLastHeartBeat() < 2 * 60 * 1000) {
        alertService.activeDelegateUpdated(updatedDelegate.getAccountId(), updatedDelegate.getUuid());
      }

      return updatedDelegate;
    }
  }

  @Override
  public Delegate updateScopes(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = wingsPersistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, DelegateKeys.includeScopes, delegate.getIncludeScopes());
    setUnset(updateOperations, DelegateKeys.excludeScopes, delegate.getExcludeScopes());

    logger.info("Updating delegate scopes : Delegate:{} includeScopes:{} excludeScopes:{}", delegate.getUuid(),
        delegate.getIncludeScopes(), delegate.getExcludeScopes());

    if (ECS.equals(delegate.getDelegateType())) {
      return updateAllDelegatesIfECSType(delegate, updateOperations, "SCOPES");
    } else {
      Delegate updatedDelegate = updateDelegate(delegate, updateOperations);
      if (System.currentTimeMillis() - updatedDelegate.getLastHeartBeat() < 2 * 60 * 1000) {
        alertService.activeDelegateUpdated(updatedDelegate.getAccountId(), updatedDelegate.getUuid());
      }
      return updatedDelegate;
    }
  }

  private Delegate updateDelegate(Delegate delegate, UpdateOperations<Delegate> updateOperations) {
    Delegate previousDelegate = get(delegate.getAccountId(), delegate.getUuid(), false);

    if (previousDelegate != null && isBlank(delegate.getDelegateProfileId())) {
      updateOperations.unset(DelegateKeys.profileResult)
          .unset(DelegateKeys.profileError)
          .unset(DelegateKeys.profileExecutedAt);

      DelegateProfileErrorAlert alertData = DelegateProfileErrorAlert.builder()
                                                .accountId(delegate.getAccountId())
                                                .hostName(delegate.getHostName())
                                                .ip(delegate.getIp())
                                                .build();
      alertService.closeAlert(delegate.getAccountId(), GLOBAL_APP_ID, AlertType.DelegateProfileError, alertData);

      if (isNotBlank(previousDelegate.getProfileResult())) {
        try {
          fileService.deleteFile(previousDelegate.getProfileResult(), FileBucket.PROFILE_RESULTS);
        } catch (MongoGridFSException e) {
          logger.warn("Didn't find profile result file: {}", previousDelegate.getProfileResult());
        }
      }
    }

    wingsPersistence.update(wingsPersistence.createQuery(Delegate.class)
                                .filter(DelegateKeys.accountId, delegate.getAccountId())
                                .filter(DelegateKeys.uuid, delegate.getUuid()),
        updateOperations);
    touchExecutingTasks(delegate);

    eventEmitter.send(Channel.DELEGATES,
        anEvent().withOrgId(delegate.getAccountId()).withUuid(delegate.getUuid()).withType(Type.UPDATE).build());
    return get(delegate.getAccountId(), delegate.getUuid(), true);
  }

  private void touchExecutingTasks(Delegate delegate) {
    // Touch currently executing tasks.
    if (isNotEmpty(delegate.getCurrentlyExecutingDelegateTasks())) {
      logger.info("Updating tasks");

      Query<DelegateTask> delegateTaskQuery = wingsPersistence.createQuery(DelegateTask.class)
                                                  .filter(DelegateTaskKeys.accountId, delegate.getAccountId())
                                                  .filter(DelegateTaskKeys.delegateId, delegate.getUuid())
                                                  .filter(DelegateTaskKeys.status, DelegateTask.Status.STARTED)
                                                  .field(DelegateTaskKeys.lastUpdatedAt)
                                                  .lessThan(System.currentTimeMillis())
                                                  .field(ID_KEY)
                                                  .in(delegate.getCurrentlyExecutingDelegateTasks());
      wingsPersistence.update(delegateTaskQuery, wingsPersistence.createUpdateOperations(DelegateTask.class));
    }
  }

  @Override
  public DelegateScripts getDelegateScripts(String accountId, String version, String managerHost,
      String verificationHost) throws IOException, TemplateException {
    ImmutableMap<String, String> scriptParams =
        getJarAndScriptRunTimeParamMap(accountId, version, managerHost, verificationHost);

    DelegateScripts delegateScripts = DelegateScripts.builder().version(version).doUpgrade(false).build();
    if (isNotEmpty(scriptParams)) {
      logger.info("Upgrading delegate to version: {}", scriptParams.get("upgradeVersion"));
      delegateScripts.setDoUpgrade(true);
      delegateScripts.setVersion(scriptParams.get("upgradeVersion"));

      try (StringWriter stringWriter = new StringWriter()) {
        cfg.getTemplate("start.sh.ftl").process(scriptParams, stringWriter);
        delegateScripts.setStartScript(stringWriter.toString());
      }
      try (StringWriter stringWriter = new StringWriter()) {
        cfg.getTemplate("delegate.sh.ftl").process(scriptParams, stringWriter);
        delegateScripts.setDelegateScript(stringWriter.toString());
      }
      try (StringWriter stringWriter = new StringWriter()) {
        cfg.getTemplate("stop.sh.ftl").process(scriptParams, stringWriter);
        delegateScripts.setStopScript(stringWriter.toString());
      }
      try (StringWriter stringWriter = new StringWriter()) {
        cfg.getTemplate("setup-proxy.sh.ftl").process(scriptParams, stringWriter);
        delegateScripts.setSetupProxyScript(stringWriter.toString());
      }
    }
    return delegateScripts;
  }

  @Override
  public String getLatestDelegateVersion(String accountId) {
    String delegateMatadata = null;
    try {
      delegateMatadata = delegateVersionCache.get(accountId);
    } catch (ExecutionException e) {
      logger.error("Execution exception", e);
    }
    return substringBefore(delegateMatadata, " ").trim();
  }

  private String fetchDelegateMetadataFromStorage() {
    String delegateMetadataUrl = mainConfiguration.getDelegateMetadataUrl().trim();
    try {
      logger.info("Fetching delegate metadata from storage: {}", delegateMetadataUrl);
      String result = Http.getResponseStringFromUrl(delegateMetadataUrl, 10, 10).trim();
      logger.info("Received from storage: {}", result);
      return result;
    } catch (IOException e) {
      logger.warn("Exception in fetching delegate version", e);
    }
    return null;
  }

  private ImmutableMap<String, String> getJarAndScriptRunTimeParamMap(
      String accountId, String version, String managerHost, String verificationHost) {
    return getJarAndScriptRunTimeParamMap(accountId, version, managerHost, verificationHost, null, null);
  }

  private ImmutableMap<String, String> getJarAndScriptRunTimeParamMap(String accountId, String version,
      String managerHost, String verificationHost, String delegateName, String delegateProfile) {
    String latestVersion = null;
    String jarRelativePath;
    String delegateJarDownloadUrl = null;
    String delegateStorageUrl = null;
    String delegateCheckLocation = null;
    boolean jarFileExists = false;
    boolean versionChanged = false;
    String delegateDockerImage = "harness/delegate:latest";

    try {
      String delegateMetadataUrl = mainConfiguration.getDelegateMetadataUrl().trim();
      delegateStorageUrl = delegateMetadataUrl.substring(0, delegateMetadataUrl.lastIndexOf('/'));
      delegateCheckLocation = delegateMetadataUrl.substring(delegateMetadataUrl.lastIndexOf('/') + 1);

      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
        logger.info("Multi-Version is enabled");
        latestVersion = version;
        String minorVersion = getMinorVersion(version).toString();
        delegateJarDownloadUrl = infraDownloadService.getDownloadUrlForDelegate(minorVersion);
        versionChanged = true;
      } else {
        logger.info("Delegate metadata URL is " + delegateMetadataUrl);
        String delegateMatadata = delegateVersionCache.get(accountId);
        logger.info("Delegate metadata: [{}]", delegateMatadata);
        latestVersion = substringBefore(delegateMatadata, " ").trim();
        jarRelativePath = substringAfter(delegateMatadata, " ").trim();
        delegateJarDownloadUrl = delegateStorageUrl + "/" + jarRelativePath;
        versionChanged = !(Version.valueOf(version).equals(Version.valueOf(latestVersion)));
      }

      if (versionChanged) {
        int responseCode = Http.getUnsafeOkHttpClient(delegateJarDownloadUrl, 10, 10)
                               .newCall(new Builder().url(delegateJarDownloadUrl).head().build())
                               .execute()
                               .code();
        logger.info("HEAD on downloadUrl got statusCode {}", responseCode);
        jarFileExists = responseCode == 200;
        logger.info("jarFileExists [{}]", jarFileExists);
      }
    } catch (IOException | ExecutionException e) {
      logger.warn("Unable to fetch delegate version information", e);
      logger.warn("CurrentVersion: [{}], LatestVersion=[{}], delegateJarDownloadUrl=[{}]", version, latestVersion,
          delegateJarDownloadUrl);
    }

    logger.info("Found delegate latest version: [{}] url: [{}]", latestVersion, delegateJarDownloadUrl);
    if (versionChanged && jarFileExists) {
      String watcherMetadataUrl = mainConfiguration.getWatcherMetadataUrl().trim();
      String watcherStorageUrl = watcherMetadataUrl.substring(0, watcherMetadataUrl.lastIndexOf('/'));
      String watcherCheckLocation = watcherMetadataUrl.substring(watcherMetadataUrl.lastIndexOf('/') + 1);

      Account account = accountService.get(accountId);

      String hexkey = String.format("%040x", new BigInteger(1, accountId.substring(0, 6).getBytes(Charsets.UTF_8)))
                          .replaceFirst("^0+(?!$)", "");

      if (mainConfiguration.getDeployMode().equals(DeployMode.KUBERNETES_ONPREM)) {
        delegateDockerImage = mainConfiguration.getPortal().getDelegateDockerImage();
      }

      ImmutableMap.Builder<String, String> params = ImmutableMap.<String, String>builder()
                                                        .put("delegateDockerImage", delegateDockerImage)
                                                        .put("accountId", accountId)
                                                        .put("accountSecret", account.getAccountKey())
                                                        .put("hexkey", hexkey)
                                                        .put("upgradeVersion", latestVersion)
                                                        .put("managerHostAndPort", managerHost)
                                                        .put("verificationHostAndPort", verificationHost)
                                                        .put("watcherStorageUrl", watcherStorageUrl)
                                                        .put("watcherCheckLocation", watcherCheckLocation)
                                                        .put("delegateStorageUrl", delegateStorageUrl)
                                                        .put("delegateCheckLocation", delegateCheckLocation)
                                                        .put("deployMode", mainConfiguration.getDeployMode().name())
                                                        .put("kubectlVersion", mainConfiguration.getKubectlVersion())
                                                        .put("kubernetesAccountLabel", getAccountIdentifier(accountId));
      if (isNotBlank(delegateName)) {
        params.put("delegateName", delegateName);
      }
      if (delegateProfile != null) {
        params.put("delegateProfile", delegateProfile);
      }

      return params.build();
    }

    logger.info("returning null paramMap");
    return null;
  }

  private Integer getMinorVersion(String delegateVersion) {
    Integer delegateVersionNumber = null;
    if (isNotBlank(delegateVersion)) {
      try {
        delegateVersionNumber = Integer.parseInt(delegateVersion.substring(delegateVersion.lastIndexOf('.') + 1));
      } catch (NumberFormatException e) {
        // Leave it null
      }
    }
    return delegateVersionNumber;
  }

  @Override
  public File downloadScripts(String managerHost, String verificationUrl, String accountId)
      throws IOException, TemplateException {
    File delegateFile = File.createTempFile(DELEGATE_DIR, ".tar");

    try (TarArchiveOutputStream out = new TarArchiveOutputStream(new FileOutputStream(delegateFile))) {
      out.putArchiveEntry(new TarArchiveEntry(DELEGATE_DIR + "/"));
      out.closeArchiveEntry();

      String version;
      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
        List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
        version = delegateVersions.get(delegateVersions.size() - 1);
      } else {
        version = "0.0.0";
      }

      ImmutableMap<String, String> scriptParams =
          getJarAndScriptRunTimeParamMap(accountId, version, managerHost, verificationUrl);

      File start = File.createTempFile("start", ".sh");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(start), UTF_8)) {
        cfg.getTemplate("start.sh.ftl").process(scriptParams, fileWriter);
      }
      start = new File(start.getAbsolutePath());
      TarArchiveEntry startTarArchiveEntry = new TarArchiveEntry(start, DELEGATE_DIR + "/start.sh");
      startTarArchiveEntry.setMode(0755);
      out.putArchiveEntry(startTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(start)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File delegate = File.createTempFile("delegate", ".sh");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(delegate), UTF_8)) {
        cfg.getTemplate("delegate.sh.ftl").process(scriptParams, fileWriter);
      }
      delegate = new File(delegate.getAbsolutePath());
      TarArchiveEntry delegateTarArchiveEntry = new TarArchiveEntry(delegate, DELEGATE_DIR + "/delegate.sh");
      delegateTarArchiveEntry.setMode(0755);
      out.putArchiveEntry(delegateTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(delegate)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File stop = File.createTempFile("stop", ".sh");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(stop), UTF_8)) {
        cfg.getTemplate("stop.sh.ftl").process(scriptParams, fileWriter);
      }
      stop = new File(stop.getAbsolutePath());
      TarArchiveEntry stopTarArchiveEntry = new TarArchiveEntry(stop, DELEGATE_DIR + "/stop.sh");
      stopTarArchiveEntry.setMode(0755);
      out.putArchiveEntry(stopTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(stop)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File setupProxy = File.createTempFile("setup-proxy", ".sh");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(setupProxy), UTF_8)) {
        cfg.getTemplate("setup-proxy.sh.ftl").process(scriptParams, fileWriter);
      }
      setupProxy = new File(setupProxy.getAbsolutePath());
      TarArchiveEntry setupProxyTarArchiveEntry = new TarArchiveEntry(setupProxy, DELEGATE_DIR + "/setup-proxy.sh");
      setupProxyTarArchiveEntry.setMode(0755);
      out.putArchiveEntry(setupProxyTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(setupProxy)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File readme = File.createTempFile("README", ".txt");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(readme), UTF_8)) {
        cfg.getTemplate("readme.txt.ftl").process(emptyMap(), fileWriter);
      }
      readme = new File(readme.getAbsolutePath());
      TarArchiveEntry readmeTarArchiveEntry = new TarArchiveEntry(readme, DELEGATE_DIR + "/README.txt");
      out.putArchiveEntry(readmeTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(readme)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      out.flush();
      out.finish();
    }

    File gzipDelegateFile = File.createTempFile(DELEGATE_DIR, ".tar.gz");
    compressGzipFile(delegateFile, gzipDelegateFile);
    return gzipDelegateFile;
  }

  private static void compressGzipFile(File file, File gzipFile) {
    try (FileInputStream fis = new FileInputStream(file); FileOutputStream fos = new FileOutputStream(gzipFile);
         GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {
      byte[] buffer = new byte[1024];
      int len;
      while ((len = fis.read(buffer)) != -1) {
        gzipOS.write(buffer, 0, len);
      }
    } catch (IOException e) {
      logger.error("Error gzipping file.", e);
    }
  }

  @Override
  public File downloadDocker(String managerHost, String verificationUrl, String accountId)
      throws IOException, TemplateException {
    File dockerDelegateFile = File.createTempFile(DOCKER_DELEGATE, ".tar");

    try (TarArchiveOutputStream out = new TarArchiveOutputStream(new FileOutputStream(dockerDelegateFile))) {
      out.putArchiveEntry(new TarArchiveEntry(DOCKER_DELEGATE + "/"));
      out.closeArchiveEntry();

      String version;
      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
        List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
        version = delegateVersions.get(delegateVersions.size() - 1);
      } else {
        version = "0.0.0";
      }

      ImmutableMap<String, String> scriptParams =
          getJarAndScriptRunTimeParamMap(accountId, version, managerHost, verificationUrl);

      File launch = File.createTempFile("launch-harness-delegate", ".sh");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(launch), UTF_8)) {
        cfg.getTemplate("launch-harness-delegate.sh.ftl").process(scriptParams, fileWriter);
      }
      launch = new File(launch.getAbsolutePath());
      TarArchiveEntry launchTarArchiveEntry =
          new TarArchiveEntry(launch, DOCKER_DELEGATE + "/launch-harness-delegate.sh");
      launchTarArchiveEntry.setMode(0755);
      out.putArchiveEntry(launchTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(launch)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File readme = File.createTempFile("README", ".txt");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(readme), UTF_8)) {
        cfg.getTemplate("readme-docker.txt.ftl").process(emptyMap(), fileWriter);
      }
      readme = new File(readme.getAbsolutePath());
      TarArchiveEntry readmeTarArchiveEntry = new TarArchiveEntry(readme, DOCKER_DELEGATE + "/README.txt");
      out.putArchiveEntry(readmeTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(readme)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      out.flush();
      out.finish();
    }

    File gzipDockerDelegateFile = File.createTempFile(DELEGATE_DIR, ".tar.gz");
    compressGzipFile(dockerDelegateFile, gzipDockerDelegateFile);
    return gzipDockerDelegateFile;
  }

  @Override
  public File downloadKubernetes(String managerHost, String verificationUrl, String accountId, String delegateName,
      String delegateProfile) throws IOException, TemplateException {
    File kubernetesDelegateFile = File.createTempFile(KUBERNETES_DELEGATE, ".tar");

    try (TarArchiveOutputStream out = new TarArchiveOutputStream(new FileOutputStream(kubernetesDelegateFile))) {
      out.putArchiveEntry(new TarArchiveEntry(KUBERNETES_DELEGATE + "/"));
      out.closeArchiveEntry();

      String version;
      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
        List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
        version = delegateVersions.get(delegateVersions.size() - 1);
      } else {
        version = "0.0.0";
      }

      ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(accountId, version, managerHost,
          verificationUrl, delegateName, delegateProfile == null ? "" : delegateProfile);

      File yaml = File.createTempFile("harness-delegate", ".yaml");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(yaml), UTF_8)) {
        cfg.getTemplate("harness-delegate.yaml.ftl").process(scriptParams, fileWriter);
      }
      yaml = new File(yaml.getAbsolutePath());
      TarArchiveEntry yamlTarArchiveEntry = new TarArchiveEntry(yaml, KUBERNETES_DELEGATE + "/harness-delegate.yaml");
      out.putArchiveEntry(yamlTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(yaml)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File readme = File.createTempFile("README", ".txt");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(readme), UTF_8)) {
        cfg.getTemplate("readme-kubernetes.txt.ftl").process(emptyMap(), fileWriter);
      }
      readme = new File(readme.getAbsolutePath());
      TarArchiveEntry readmeTarArchiveEntry = new TarArchiveEntry(readme, KUBERNETES_DELEGATE + "/README.txt");
      out.putArchiveEntry(readmeTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(readme)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      out.flush();
      out.finish();
    }

    File gzipKubernetesDelegateFile = File.createTempFile(DELEGATE_DIR, ".tar.gz");
    compressGzipFile(kubernetesDelegateFile, gzipKubernetesDelegateFile);

    return gzipKubernetesDelegateFile;
  }

  @Override
  public File downloadDelegateValuesYamlFile(String managerHost, String verificationUrl, String accountId,
      String delegateName, String delegateProfile) throws IOException, TemplateException {
    String version;

    if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
      List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
      version = delegateVersions.get(delegateVersions.size() - 1);
    } else {
      version = "0.0.0";
    }

    ImmutableMap<String, String> params = getJarAndScriptRunTimeParamMap(
        accountId, version, managerHost, verificationUrl, delegateName, delegateProfile == null ? "" : delegateProfile);

    File yaml = File.createTempFile(HARNESS_DELEGATE_VALUES_YAML, ".yaml");
    try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(yaml), UTF_8)) {
      cfg.getTemplate("delegate-helm-values.yaml.ftl").process(params, fileWriter);
    }

    return yaml;
  }

  @Override
  public File downloadECSDelegate(String managerHost, String verificationUrl, String accountId, boolean awsVpcMode,
      String hostname, String delegateGroupName, String delegateProfile) throws IOException, TemplateException {
    File ecsDelegateFile = File.createTempFile(ECS_DELEGATE, ".tar");

    try (TarArchiveOutputStream out = new TarArchiveOutputStream(new FileOutputStream(ecsDelegateFile))) {
      out.putArchiveEntry(new TarArchiveEntry(ECS_DELEGATE + "/"));
      out.closeArchiveEntry();

      String version;
      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
        List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
        version = delegateVersions.get(delegateVersions.size() - 1);
      } else {
        version = "0.0.0";
      }

      ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(accountId, version, managerHost,
          verificationUrl, StringUtils.EMPTY, delegateProfile == null ? "" : delegateProfile);
      scriptParams = updateMapForEcsDelegate(awsVpcMode, hostname, delegateGroupName, scriptParams);

      // Add Task Spec Json file
      File yaml = File.createTempFile("ecs-spec", ".json");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(yaml), UTF_8)) {
        cfg.getTemplate("harness-ecs-delegate.json.ftl").process(scriptParams, fileWriter);
      }
      yaml = new File(yaml.getAbsolutePath());
      TarArchiveEntry yamlTarArchiveEntry = new TarArchiveEntry(yaml, ECS_DELEGATE + "/ecs-task-spec.json");
      out.putArchiveEntry(yamlTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(yaml)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      // Add Task "Service Spec Json for awsvpc mode" file
      File serviceJson = File.createTempFile("ecs-service-spec", ".json");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(serviceJson), UTF_8)) {
        cfg.getTemplate("harness-ecs-delegate-service.json.ftl").process(scriptParams, fileWriter);
      }
      serviceJson = new File(serviceJson.getAbsolutePath());
      TarArchiveEntry serviceJsonTarArchiveEntry =
          new TarArchiveEntry(serviceJson, ECS_DELEGATE + "/service-spec-for-awsvpc-mode.json");
      out.putArchiveEntry(serviceJsonTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(serviceJson)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      // Add Readme file
      File readme = File.createTempFile("README", ".txt");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(readme), UTF_8)) {
        cfg.getTemplate("readme-ecs.txt.ftl").process(emptyMap(), fileWriter);
      }
      readme = new File(readme.getAbsolutePath());
      TarArchiveEntry readmeTarArchiveEntry = new TarArchiveEntry(readme, ECS_DELEGATE + "/README.txt");
      out.putArchiveEntry(readmeTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(readme)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      out.flush();
      out.finish();
    }

    File gzipEcsDelegateFile = File.createTempFile(DELEGATE_DIR, ".tar.gz");
    compressGzipFile(ecsDelegateFile, gzipEcsDelegateFile);

    return gzipEcsDelegateFile;
  }

  private ImmutableMap<String, String> updateMapForEcsDelegate(
      boolean awsVpcMode, String hostname, String delegateGroupName, ImmutableMap<String, String> scriptParams) {
    Map<String, String> map = new HashMap<>(scriptParams);
    // AWSVPC mode, hostname must be null
    if (awsVpcMode) {
      map.put("networkModeForTask", "\"networkMode\": \"awsvpc\",");
      map.put("hostnameForDelegate", StringUtils.EMPTY);
    } else {
      map.put("networkModeForTask", StringUtils.EMPTY);
      if (isBlank(hostname)) {
        // hostname not provided, use as null, so dockerId will become hostname in ecs
        hostname = HARNESS_ECS_DELEGATE;
      }
      map.put("hostnameForDelegate", "\"hostname\": \"" + hostname + "\",");
    }

    map.put("delegateGroupName", delegateGroupName);

    return ImmutableMap.copyOf(map);
  }

  @Override
  public Delegate add(Delegate delegate) {
    Delegate savedDelegate;
    String accountId = delegate.getAccountId();
    int maxUsageAllowed = delegatesFeature.getMaxUsageAllowedForAccount(accountId);
    if (maxUsageAllowed != Integer.MAX_VALUE) {
      try (AcquiredLock ignored =
               persistentLocker.acquireLock("delegateCountLock-" + accountId, Duration.ofMinutes(3))) {
        long currentDelegateCount = getTotalNumberOfDelegates(accountId);
        if (currentDelegateCount < maxUsageAllowed) {
          savedDelegate = saveDelegate(delegate);
        } else {
          throw new WingsException(ErrorCode.USAGE_LIMITS_EXCEEDED,
              String.format(
                  "Can not add delegate to the account. Maximum [%d] delegates are supported", maxUsageAllowed),
              USER);
        }
      }
    } else {
      savedDelegate = saveDelegate(delegate);
    }

    logger.info("Delegate saved: {}", savedDelegate);

    // When polling is enabled for delegate, do not perform these event publishing
    if (isDelegateWithoutPollingEnabled(delegate)) {
      eventEmitter.send(Channel.DELEGATES,
          anEvent().withOrgId(delegate.getAccountId()).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
      assignDelegateService.clearConnectionResults(delegate.getAccountId());
      eventPublishHelper.publishInstalledDelegateEvent(delegate.getAccountId(), delegate.getUuid());
    }

    updateWithTokenAndSeqNumIfEcsDelegate(delegate, savedDelegate);
    return savedDelegate;
  }

  private long getTotalNumberOfDelegates(String accountId) {
    return wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.accountId, accountId).count();
  }

  private Delegate saveDelegate(Delegate delegate) {
    logger.info("Adding delegate {} for account {}", delegate.getHostName(), delegate.getAccountId());
    wingsPersistence.save(delegate);
    logger.info("Delegate saved: {}", delegate);
    return delegate;
  }

  @Override
  public void delete(String accountId, String delegateId) {
    logger.info("Deleting delegate: {}", delegateId);
    Delegate existingDelegate = wingsPersistence.createQuery(Delegate.class)
                                    .filter(DelegateKeys.accountId, accountId)
                                    .filter(DelegateKeys.uuid, delegateId)
                                    .project(DelegateKeys.ip, true)
                                    .project(DelegateKeys.hostName, true)
                                    .get();

    if (existingDelegate != null) {
      // before deleting delegate, check if any alert is open for delegate, if yes, close it.
      alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.DelegatesDown,
          DelegatesDownAlert.builder()
              .accountId(accountId)
              .ip(existingDelegate.getIp())
              .hostName(existingDelegate.getHostName())
              .build());
      alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.DelegateProfileError,
          DelegateProfileErrorAlert.builder()
              .accountId(accountId)
              .ip(existingDelegate.getIp())
              .hostName(existingDelegate.getHostName())
              .build());
    }

    wingsPersistence.delete(wingsPersistence.createQuery(Delegate.class)
                                .filter(DelegateKeys.accountId, accountId)
                                .filter(DelegateKeys.uuid, delegateId));
  }

  @Override
  public void retainOnlySelectedDelegatesAndDeleteRest(String accountId, List<String> delegatesToRetain) {
    if (EmptyPredicate.isNotEmpty(delegatesToRetain)) {
      wingsPersistence.delete(wingsPersistence.createQuery(Delegate.class)
                                  .filter(DelegateKeys.accountId, accountId)
                                  .field(DelegateKeys.uuid)
                                  .notIn(delegatesToRetain));
    } else {
      logger.info("List of delegates to retain is empty. In order to delete delegates, pass a list of delegate IDs");
    }
  }

  @Override
  public Delegate register(Delegate delegate) {
    if (licenseService.isAccountDeleted(delegate.getAccountId())) {
      broadcasterFactory.lookup("/stream/delegate/" + delegate.getAccountId(), true).broadcast(SELF_DESTRUCT);
      return Delegate.builder().uuid(SELF_DESTRUCT).build();
    }

    Query<Delegate> delegateQuery = wingsPersistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.accountId, delegate.getAccountId())
                                        .filter(DelegateKeys.hostName, delegate.getHostName());
    // For delegates running in a kubernetes cluster we include lowercase account ID in the hostname to identify it.
    // We ignore IP address because that can change with every restart of the pod.
    if (!delegate.getHostName().contains(getAccountIdentifier(delegate.getAccountId()))) {
      delegateQuery.filter(DelegateKeys.ip, delegate.getIp());
    }

    Delegate existingDelegate =
        delegateQuery.project(DelegateKeys.status, true).project(DelegateKeys.delegateProfileId, true).get();
    if (existingDelegate != null && existingDelegate.getStatus() == Status.DELETED) {
      broadcasterFactory.lookup("/stream/delegate/" + delegate.getAccountId(), true)
          .broadcast(SELF_DESTRUCT + "-" + existingDelegate.getUuid());

      return Delegate.builder().uuid(SELF_DESTRUCT).build();
    }

    logger.info("Registering delegate for account {}: Hostname: {} IP: {}", delegate.getAccountId(),
        delegate.getHostName(), delegate.getIp());

    if (ECS.equals(delegate.getDelegateType())) {
      return handleEcsDelegateRequest(delegate);
    } else {
      return upsertDelegateOperation(existingDelegate, delegate);
    }
  }

  @VisibleForTesting
  Delegate upsertDelegateOperation(Delegate existingDelegate, Delegate delegate) {
    long delegateHeartbeat = delegate.getLastHeartBeat();
    long now = clock.millis();
    long skew = Math.abs(now - delegateHeartbeat);
    if (skew > TimeUnit.MINUTES.toMillis(2L)) {
      logger.warn("Delegate {} has clock skew of {}", delegate.getUuid(), Misc.getDurationString(skew));
    }
    delegate.setLastHeartBeat(now);
    Delegate registeredDelegate;
    if (existingDelegate == null) {
      logger.info("No existing delegate, adding for account {}: Hostname: {} IP: {}", delegate.getAccountId(),
          delegate.getHostName(), delegate.getIp());
      registeredDelegate = add(delegate);
    } else {
      logger.info("Delegate exists, updating: {}", delegate.getUuid());
      delegate.setUuid(existingDelegate.getUuid());
      delegate.setStatus(existingDelegate.getStatus());
      delegate.setDelegateProfileId(existingDelegate.getDelegateProfileId());
      if (ECS.equals(delegate.getDelegateType())) {
        registeredDelegate = updateEcsDelegate(delegate, false);
      } else {
        registeredDelegate = update(delegate);
      }
    }

    // Not needed to be done when polling is enabled for delegate
    if (isDelegateWithoutPollingEnabled(delegate)) {
      // Broadcast Message containing, DelegateId and SeqNum (if applicable)
      StringBuilder message = new StringBuilder(128).append("[X]").append(delegate.getUuid());
      updateBroadcastMessageIfEcsDelegate(message, delegate, registeredDelegate);
      broadcasterFactory.lookup("/stream/delegate/" + delegate.getAccountId(), true).broadcast(message.toString());

      alertService.activeDelegateUpdated(registeredDelegate.getAccountId(), registeredDelegate.getUuid());
      registeredDelegate.setVerificationServiceSecret(learningEngineService.getServiceSecretKey(LEARNING_ENGINE));
      registeredDelegate.setEventServiceSecret(learningEngineService.getServiceSecretKey(EVENT_SERVICE));
    }
    return registeredDelegate;
  }

  private void updateBroadcastMessageIfEcsDelegate(
      StringBuilder message, Delegate delegate, Delegate registeredDelegate) {
    if (ECS.equals(delegate.getDelegateType())) {
      String hostName = getDelegateHostNameByRemovingSeqNum(registeredDelegate);
      String seqNum = getDelegateSeqNumFromHostName(registeredDelegate);
      DelegateSequenceConfig sequenceConfig =
          getDelegateSequenceConfig(delegate.getAccountId(), hostName, Integer.parseInt(seqNum));
      registeredDelegate.setDelegateRandomToken(sequenceConfig.getDelegateToken());
      registeredDelegate.setSequenceNum(sequenceConfig.getSequenceNum().toString());
      message.append("[TOKEN]")
          .append(sequenceConfig.getDelegateToken())
          .append("[SEQ]")
          .append(sequenceConfig.getSequenceNum());

      logger.info("^^^^SEQ: " + message.toString());
    }
  }

  @VisibleForTesting
  DelegateSequenceConfig getDelegateSequenceConfig(String accountId, String hostName, Integer seqNum) {
    Query<DelegateSequenceConfig> delegateSequenceQuery = wingsPersistence.createQuery(DelegateSequenceConfig.class)
                                                              .filter(DelegateSequenceConfigKeys.accountId, accountId)
                                                              .filter(DelegateSequenceConfigKeys.hostName, hostName);

    if (seqNum != null) {
      delegateSequenceQuery.filter(DelegateSequenceConfigKeys.sequenceNum, seqNum);
    }

    return delegateSequenceQuery.project(DelegateSequenceConfigKeys.accountId, true)
        .project(DelegateSequenceConfigKeys.sequenceNum, true)
        .project(DelegateSequenceConfigKeys.hostName, true)
        .project(DelegateSequenceConfigKeys.delegateToken, true)
        .get();
  }

  @Override
  public DelegateProfileParams checkForProfile(
      String accountId, String delegateId, String profileId, long lastUpdatedAt) {
    logger.info("Checking delegate profile for account {}, delegate [{}]. Previous profile [{}] updated at {}",
        accountId, delegateId, profileId, lastUpdatedAt);
    Delegate delegate = get(accountId, delegateId, true);

    if (delegate == null) {
      return null;
    }

    if (isNotBlank(profileId) && isBlank(delegate.getDelegateProfileId())) {
      return DelegateProfileParams.builder().profileId("NONE").build();
    }

    if (isNotBlank(delegate.getDelegateProfileId())) {
      DelegateProfile profile = delegateProfileService.get(accountId, delegate.getDelegateProfileId());
      if (profile != null && (!profile.getUuid().equals(profileId) || profile.getLastUpdatedAt() > lastUpdatedAt)) {
        Map<String, Object> context = new HashMap<>();
        context.put("secrets",
            SecretFunctor.builder()
                .managerDecryptionService(managerDecryptionService)
                .secretManager(secretManager)
                .accountId(accountId)
                .build());
        String scriptContent = evaluator.substitute(profile.getStartupScript(), context);
        return DelegateProfileParams.builder()
            .profileId(profile.getUuid())
            .name(profile.getName())
            .profileLastUpdatedAt(profile.getLastUpdatedAt())
            .scriptContent(scriptContent)
            .build();
      }
    }
    return null;
  }

  @Override
  public void saveProfileResult(String accountId, String delegateId, boolean error, FileBucket fileBucket,
      InputStream uploadedInputStream, FormDataContentDisposition fileDetail) {
    Delegate delegate = get(accountId, delegateId, true);
    DelegateProfileErrorAlert alertData = DelegateProfileErrorAlert.builder()
                                              .accountId(accountId)
                                              .hostName(delegate.getHostName())
                                              .ip(delegate.getIp())
                                              .build();
    if (error) {
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.DelegateProfileError, alertData);
    } else {
      alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.DelegateProfileError, alertData);
    }

    FileMetadata fileMetadata = FileMetadata.builder()
                                    .fileName(new File(fileDetail.getFileName()).getName())
                                    .accountId(accountId)
                                    .fileUuid(UUIDGenerator.generateUuid())
                                    .build();
    String fileId = fileService.saveFile(fileMetadata,
        new BoundedInputStream(uploadedInputStream, mainConfiguration.getFileUploadLimits().getProfileResultLimit()),
        fileBucket);

    String previousProfileResult = delegate.getProfileResult();

    wingsPersistence.update(wingsPersistence.createQuery(Delegate.class)
                                .filter(DelegateKeys.accountId, accountId)
                                .filter(DelegateKeys.uuid, delegateId),
        wingsPersistence.createUpdateOperations(Delegate.class)
            .set(DelegateKeys.profileResult, fileId)
            .set(DelegateKeys.profileError, error)
            .set(DelegateKeys.profileExecutedAt, clock.millis()));

    if (isNotBlank(previousProfileResult)) {
      fileService.deleteFile(previousProfileResult, FileBucket.PROFILE_RESULTS);
    }
  }

  @Override
  public String getProfileResult(String accountId, String delegateId) {
    Delegate delegate = get(accountId, delegateId, false);

    String profileResultFileId = delegate.getProfileResult();

    if (isBlank(profileResultFileId)) {
      return "No profile result available for " + delegate.getHostName();
    }

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      fileService.downloadToStream(profileResultFileId, os, FileBucket.PROFILE_RESULTS);
      os.flush();
      return new String(os.toByteArray(), UTF_8);
    } catch (Exception e) {
      throw new WingsException(GENERAL_ERROR, e)
          .addParam("message", "Profile execution log temporarily unavailable. Try again in a few moments.");
    }
  }

  @Override
  public void removeDelegateConnection(String accountId, String delegateConnectionId) {
    logger.info(
        "Removing delegate connection for account {}: delegateConnectionId: {}", accountId, delegateConnectionId);
    wingsPersistence.delete(accountId, DelegateConnection.class, delegateConnectionId);
  }

  @Override
  public void doConnectionHeartbeat(String accountId, String delegateId, DelegateConnectionHeartbeat heartbeat) {
    UpdateResults updated = wingsPersistence.update(wingsPersistence.createQuery(DelegateConnection.class)
                                                        .filter(DelegateConnectionKeys.accountId, accountId)
                                                        .filter(ID_KEY, heartbeat.getDelegateConnectionId()),
        wingsPersistence.createUpdateOperations(DelegateConnection.class)
            .set(DelegateConnectionKeys.lastHeartbeat, System.currentTimeMillis())
            .set(DelegateConnectionKeys.validUntil,
                Date.from(OffsetDateTime.now().plusMinutes(defaultExpiryTimeInMinutes).toInstant())));

    if (updated != null && updated.getWriteResult() != null && updated.getWriteResult().getN() == 0) {
      // connection does not exist. Create one.
      DelegateConnection connection =
          DelegateConnection.builder()
              .accountId(accountId)
              .delegateId(delegateId)
              .version(heartbeat.getVersion())
              .lastHeartbeat(System.currentTimeMillis())
              .validUntil(Date.from(OffsetDateTime.now().plusMinutes(defaultExpiryTimeInMinutes).toInstant()))
              .build();
      connection.setUuid(heartbeat.getDelegateConnectionId());
      wingsPersistence.save(connection);
    }
  }

  @Override
  public String queueTask(DelegateTask task) {
    return saveAndBroadcastTask(task, true).getUuid();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ResponseData> T executeTask(DelegateTask task) {
    List<String> eligibleDelegateIds = ensureDelegateAvailableToExecuteTask(task);
    if (isEmpty(eligibleDelegateIds)) {
      throw new WingsException(UNAVAILABLE_DELEGATES, USER_ADMIN);
    }
    DelegateTask delegateTask = saveAndBroadcastTask(task, false);

    // Wait for task to complete
    DelegateTask completedTask;
    try {
      syncTaskWaitMap.put(delegateTask.getUuid(), new Object());
      synchronized (syncTaskWaitMap.get(delegateTask.getUuid())) {
        syncTaskWaitMap.get(delegateTask.getUuid()).wait(task.getData().getTimeout());
      }
      completedTask = wingsPersistence.get(DelegateTask.class, task.getUuid());
    } catch (Exception e) {
      logger.error("Exception", e);
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, e).addParam("args", "Error while waiting for completion");
    } finally {
      syncTaskWaitMap.remove(delegateTask.getUuid());
      wingsPersistence.delete(wingsPersistence.createQuery(DelegateTask.class)
                                  .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                  .filter(ID_KEY, delegateTask.getUuid()));
    }

    if (completedTask == null) {
      logger.info("Task {} was deleted while waiting for completion", delegateTask.getUuid());
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Task was deleted while waiting for completion");
    }

    ResponseData responseData = completedTask.getNotifyResponse();
    if (responseData == null || !TASK_COMPLETED_STATUSES.contains(completedTask.getStatus())) {
      throw new WingsException(ErrorCode.REQUEST_TIMEOUT, WingsException.USER_ADMIN)
          .addParam("name", "Harness delegate");
    }

    logger.info("Returned response to calling function for delegate task [{}] ", delegateTask.getUuid());
    return (T) responseData;
  }

  private DelegateTask saveAndBroadcastTask(DelegateTask task, boolean async) {
    task.setStatus(QUEUED);
    task.setAsync(async);
    task.setVersion(getVersion());
    task.setBroadcastCount(1);
    task.setLastBroadcastAt(clock.millis());
    wingsPersistence.save(task);

    generateCapabilitiesForTaskIfFeatureEnabled(task);
    String preAssignedDelegateId = assignDelegateService.pickFirstAttemptDelegate(task);

    // Update fields for DelegateTask, preAssignedDelegateId and executionCapabilities if not empty
    task = updateDelegateTaskWithPreAssignedDelegateId(task, preAssignedDelegateId, task.getExecutionCapabilities());

    logger.info("{} task: uuid: {}, accountId: {}, type: {}, correlationId: {}",
        async ? "Queueing async" : "Executing sync", task.getUuid(), task.getAccountId(), task.getData().getTaskType(),
        task.getCorrelationId());

    broadcasterFactory.lookup("/stream/delegate/" + task.getAccountId(), true).broadcast(task);
    return task;
  }

  // TODO: Required right now, as at delegateSide based on capabilities are present or not,
  // TODO: either new CapabilityCheckController or existing ValidationClass is used.
  private void generateCapabilitiesForTaskIfFeatureEnabled(DelegateTask task) {
    if (!CapabilityUtils.isTaskTypeMigratedToCapabilityFramework(task.getData().getTaskType())
        || !featureFlagService.isEnabled(DELEGATE_CAPABILITY_FRAMEWORK, task.getAccountId())) {
      return;
    }

    addMergedParamsForCapabilityCheck(task);

    DelegatePackage delegatePackage = getDelegataePackageWithEncryptionConfig(task, task.getDelegateId());
    CapabilityHelper.embedCapabilitiesInDelegateTask(task,
        delegatePackage == null || isEmpty(delegatePackage.getEncryptionConfigs())
            ? EMPTY_LIST
            : delegatePackage.getEncryptionConfigs().values());

    if (isNotEmpty(task.getExecutionCapabilities())) {
      logger.info(CapabilityHelper.generateLogStringWithCapabilitiesGenerated(task));
    }
  }

  // For some of the tasks, the necesssary factors to do capability check is split across multiple
  // params. So none of the params can provide the execution capability by itself. To work around this,
  // we're adding extra params that combines these split params.
  private void addMergedParamsForCapabilityCheck(DelegateTask task) {
    TaskType type = TaskType.valueOf(task.getData().getTaskType());
    Object[] params = task.getData().getParameters();
    if (type == HOST_VALIDATION) {
      // the host is in params[2] and port in params[3]

      HostValidationTaskParameters hostValidationTaskParameters =
          HostValidationTaskParameters.builder()
              .hostNames((List<String>) params[2])
              .connectionSetting((SettingAttribute) params[3])
              .encryptionDetails((List<EncryptedDataDetail>) params[4])
              .executionCredential((ExecutionCredential) params[5])
              .build();
      List<Object> newParams = new ArrayList<>(Arrays.asList(hostValidationTaskParameters));
      task.getData().setParameters(newParams.toArray());
    }
  }

  private List<String> ensureDelegateAvailableToExecuteTask(DelegateTask task) {
    if (task == null) {
      logger.warn("Delegate task is null");
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Delegate task is null");
    }
    if (task.getAccountId() == null) {
      logger.warn("Delegate task has null account ID");
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Delegate task has null account ID");
    }

    List<String> activeDelegates = wingsPersistence.createQuery(Delegate.class)
                                       .filter(DelegateKeys.accountId, task.getAccountId())
                                       .field(DelegateKeys.lastHeartBeat)
                                       .greaterThan(clock.millis() - MAX_DELEGATE_LAST_HEARTBEAT)
                                       .asKeyList()
                                       .stream()
                                       .map(key -> key.getId().toString())
                                       .collect(toList());

    logger.info("{} delegates {} are active", activeDelegates.size(), activeDelegates);

    List<String> eligibleDelegates = activeDelegates.stream()
                                         .filter(delegateId -> assignDelegateService.canAssign(delegateId, task))
                                         .collect(toList());

    if (activeDelegates.isEmpty()) {
      logger.info("No delegates are active for the account: {}", task.getAccountId());
      alertService.openAlert(task.getAccountId(), GLOBAL_APP_ID, AlertType.NoActiveDelegates,
          NoActiveDelegatesAlert.builder().accountId(task.getAccountId()).build());
    } else if (eligibleDelegates.isEmpty()) {
      logger.warn("{} delegates active but no delegates are eligible to execute task [{}:{}] for the accountId: {}",
          activeDelegates.size(), task.getUuid(), task.getData().getTaskType(), task.getAccountId());
      alertService.openAlert(task.getAccountId(), task.getAppId(), NoEligibleDelegates,
          NoEligibleDelegatesAlert.builder()
              .accountId(task.getAccountId())
              .appId(task.getAppId())
              .envId(task.getEnvId())
              .infraMappingId(task.getInfrastructureMappingId())
              .taskGroup(TaskType.valueOf(task.getData().getTaskType()).getTaskGroup())
              .taskType(TaskType.valueOf(task.getData().getTaskType()))
              .tags(task.getTags())
              .build());
    }

    logger.info("{} delegates {} eligible to execute task {}", eligibleDelegates.size(), eligibleDelegates,
        task.getData().getTaskType());
    return eligibleDelegates;
  }

  @Override
  public DelegatePackage acquireDelegateTask(String accountId, String delegateId, String taskId) {
    logger.info("Acquiring delegate task {} for delegate {}", taskId, delegateId);
    DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId, delegateId);
    if (delegateTask == null) {
      return null;
    }

    if (!assignDelegateService.canAssign(delegateId, delegateTask)) {
      logger.info("Delegate {} is not scoped for task {}", delegateId, taskId);
      ensureDelegateAvailableToExecuteTask(delegateTask); // Raises an alert if there are no eligible delegates.
      return null;
    }

    if (assignDelegateService.isWhitelisted(delegateTask, delegateId)) {
      return assignTask(delegateId, taskId, delegateTask);
    } else if (assignDelegateService.shouldValidate(delegateTask, delegateId)) {
      setValidationStarted(delegateId, delegateTask);
      return DelegatePackage.builder().delegateTask(delegateTask).build();
    } else {
      logger.info("Delegate {} is blacklisted for task {}", delegateId, taskId);
      return null;
    }
  }

  @Override
  public DelegatePackage reportConnectionResults(
      String accountId, String delegateId, String taskId, List<DelegateConnectionResult> results) {
    assignDelegateService.saveConnectionResults(results);
    DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId, delegateId);
    if (delegateTask == null) {
      return null;
    }

    logger.info("Delegate {} completed validating task {} {}", delegateId, delegateTask.getUuid(),
        delegateTask.isAsync() ? "(async)" : "(sync)");

    UpdateOperations<DelegateTask> updateOperations = wingsPersistence.createUpdateOperations(DelegateTask.class)
                                                          .addToSet("validationCompleteDelegateIds", delegateId);
    Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                          .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                          .filter(DelegateTaskKeys.status, QUEUED)
                                          .field(DelegateTaskKeys.delegateId)
                                          .doesNotExist()
                                          .filter(ID_KEY, delegateTask.getUuid());
    wingsPersistence.update(updateQuery, updateOperations);

    if (results.stream().anyMatch(DelegateConnectionResult::isValidated)) {
      return assignTask(delegateId, taskId, delegateTask);
    }
    return null;
  }

  private DelegateTask updateDelegateTaskWithPreAssignedDelegateId(
      DelegateTask delegateTask, String preAssignedDelegateId, List<ExecutionCapability> executionCapabilities) {
    if (isBlank(preAssignedDelegateId) && isEmpty(executionCapabilities)) {
      // Reason here we are fetching delegateTask again from DB  is,
      // Before this call is made, we try to generate Capabilities required for this delegate Task.
      // During this, we try to evaluateExpressions, which will replace secrets in parameters with expressions,
      // like secretFunctor.obtain(),
      // We want to broadcast original DelegateTask and not this modified one.
      // So here we fetch original task and return it, so it will be broadcasted.
      return wingsPersistence.get(DelegateTask.class, delegateTask.getUuid());
    }

    Query<DelegateTask> query = wingsPersistence.createQuery(DelegateTask.class)
                                    .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                    .filter(DelegateTaskKeys.status, QUEUED)
                                    .field(DelegateTaskKeys.delegateId)
                                    .doesNotExist()
                                    .filter(ID_KEY, delegateTask.getUuid());

    UpdateOperations<DelegateTask> updateOperations = wingsPersistence.createUpdateOperations(DelegateTask.class);

    if (isNotBlank(preAssignedDelegateId)) {
      updateOperations.set(DelegateTaskKeys.preAssignedDelegateId, preAssignedDelegateId);
    }

    if (isNotEmpty(executionCapabilities)) {
      updateOperations.set(DelegateTaskKeys.executionCapabilities, executionCapabilities);
    }

    return wingsPersistence.findAndModifySystemData(query, updateOperations, HPersistence.returnNewOptions);
  }

  @Override
  public DelegateTask failIfAllDelegatesFailed(String accountId, String delegateId, String taskId) {
    DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId, delegateId);
    if (delegateTask == null) {
      logger.info("Task {} not found or was already assigned", taskId);
      return null;
    }
    if (isValidationComplete(delegateTask)) {
      // Check whether a whitelisted delegate is connected
      List<String> whitelistedDelegates = assignDelegateService.connectedWhitelistedDelegates(delegateTask);
      if (isNotEmpty(whitelistedDelegates)) {
        logger.info("Waiting for task {} to be acquired by a whitelisted delegate: {}", taskId, whitelistedDelegates);
        return null;
      } else {
        logger.info("No whitelisted delegates found for task {}", taskId);
        List<String> criteria =
            TaskType.valueOf(delegateTask.getData().getTaskType()).getCriteria(delegateTask, injector);
        String errorMessage = "No delegates could reach the resource. " + criteria;
        logger.info("Task {}: {}", taskId, errorMessage);
        ResponseData response;
        if (delegateTask.isAsync()) {
          response = ErrorNotifyResponseData.builder().errorMessage(errorMessage).build();
        } else {
          InvalidRequestException exception = new InvalidRequestException(errorMessage, USER);
          response = aRemoteMethodReturnValueData().withException(exception).build();
        }
        processDelegateResponse(accountId, null, taskId,
            DelegateTaskResponse.builder()
                .accountId(accountId)
                .response(response)
                .responseCode(ResponseCode.OK)
                .build());
      }
    }

    logger.info("Task {} is still being validated", taskId);
    return null;
  }

  private void setValidationStarted(String delegateId, DelegateTask delegateTask) {
    logger.info("Delegate {} to validate task {} {}", delegateId, delegateTask.getUuid(),
        delegateTask.isAsync() ? "(async)" : "(sync)");
    UpdateOperations<DelegateTask> updateOperations = wingsPersistence.createUpdateOperations(DelegateTask.class)
                                                          .addToSet(DelegateTaskKeys.validatingDelegateIds, delegateId);
    Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                          .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                          .filter(DelegateTaskKeys.status, QUEUED)
                                          .field(DelegateTaskKeys.delegateId)
                                          .doesNotExist()
                                          .filter(ID_KEY, delegateTask.getUuid());
    wingsPersistence.update(updateQuery, updateOperations);

    wingsPersistence.update(updateQuery.field(DelegateTaskKeys.validationStartedAt).doesNotExist(),
        wingsPersistence.createUpdateOperations(DelegateTask.class)
            .set(DelegateTaskKeys.validationStartedAt, clock.millis()));
  }

  private boolean isValidationComplete(DelegateTask delegateTask) {
    Set<String> validatingDelegates = delegateTask.getValidatingDelegateIds();
    Set<String> completeDelegates = delegateTask.getValidationCompleteDelegateIds();
    boolean allDelegatesFinished = isNotEmpty(validatingDelegates) && isNotEmpty(completeDelegates)
        && completeDelegates.containsAll(validatingDelegates);
    if (allDelegatesFinished) {
      logger.info("Validation attempts are complete for task {}", delegateTask.getUuid());
    }
    boolean validationTimedOut = delegateTask.getValidationStartedAt() != null
        && clock.millis() - delegateTask.getValidationStartedAt() > VALIDATION_TIMEOUT;
    if (validationTimedOut) {
      logger.info("Validation timed out for task {}", delegateTask.getUuid());
    }
    return allDelegatesFinished || validationTimedOut;
  }

  private void clearFromValidationCache(DelegateTask delegateTask) {
    UpdateOperations<DelegateTask> updateOperations = wingsPersistence.createUpdateOperations(DelegateTask.class)
                                                          .unset(DelegateTaskKeys.validatingDelegateIds)
                                                          .unset(DelegateTaskKeys.validationCompleteDelegateIds);
    Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                          .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                          .filter(DelegateTaskKeys.status, QUEUED)
                                          .field(DelegateTaskKeys.delegateId)
                                          .doesNotExist()
                                          .filter(ID_KEY, delegateTask.getUuid());
    wingsPersistence.update(updateQuery, updateOperations);
  }

  private DelegateTask getUnassignedDelegateTask(String accountId, String taskId, String delegateId) {
    DelegateTask delegateTask = wingsPersistence.createQuery(DelegateTask.class)
                                    .filter(DelegateTaskKeys.accountId, accountId)
                                    .filter(ID_KEY, taskId)
                                    .get();

    if (delegateTask != null) {
      if (delegateTask.getDelegateId() == null && delegateTask.getStatus() == QUEUED) {
        logger.info("Found unassigned delegate task: {}", delegateTask.getUuid());
        return delegateTask;
      } else if (delegateId.equals(delegateTask.getDelegateId())) {
        logger.info("Returning already assigned task {} to delegate {} from getUnassigned", taskId, delegateId);
        return delegateTask;
      }
      logger.info("Task {} not available for delegate {} - it was assigned to {} and has status {}", taskId, delegateId,
          delegateTask.getDelegateId(), delegateTask.getStatus());
    } else {
      logger.info("Task {} no longer exists", taskId);
    }
    return null;
  }

  private DelegatePackage resolvePreAssignmentExpressions(DelegateTask delegateTask, String delegateId) {
    try {
      final ManagerPreExecutionExpressionEvaluator managerPreExecutionExpressionEvaluator =
          new ManagerPreExecutionExpressionEvaluator(serviceTemplateService, configService, delegateTask.getAppId(),
              delegateTask.getEnvId(), delegateTask.getServiceTemplateId(), artifactCollectionUtils,
              delegateTask.getArtifactStreamId(), managerDecryptionService, secretManager, delegateTask.getAccountId(),
              delegateTask.getWorkflowExecutionId(), delegateTask.getData().getExpressionFunctorToken());

      if (delegateTask.getData().getParameters().length == 1
          && delegateTask.getData().getParameters()[0] instanceof TaskParameters) {
        logger.info("Applying ManagerPreExecutionExpressionEvaluator for delegateTask {}", delegateTask.getUuid());
        ExpressionReflectionUtils.applyExpression(delegateTask.getData().getParameters()[0],
            value -> managerPreExecutionExpressionEvaluator.substitute(value, new HashMap<>()));

        final SecretManagerFunctor secretManagerFunctor =
            (SecretManagerFunctor) managerPreExecutionExpressionEvaluator.getSecretManagerFunctor();

        return DelegatePackage.builder()
            .delegateTask(delegateTask)
            .encryptionConfigs(secretManagerFunctor.getEncryptionConfigs())
            .secretDetails(secretManagerFunctor.getSecretDetails())
            .build();
      }

    } catch (CriticalExpressionEvaluationException exception) {
      logger.error("Exception in ManagerPreExecutionExpressionEvaluator ", exception);
      Query<DelegateTask> taskQuery = wingsPersistence.createQuery(DelegateTask.class)
                                          .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                          .filter(ID_KEY, delegateTask.getUuid());
      DelegateTaskResponse response =
          DelegateTaskResponse.builder()
              .response(ErrorNotifyResponseData.builder().errorMessage(ExceptionUtils.getMessage(exception)).build())
              .responseCode(ResponseCode.FAILED)
              .accountId(delegateTask.getAccountId())
              .build();
      handleResponse(delegateTask.getUuid(), delegateId, delegateTask, taskQuery, response, ERROR);
      return null;
    }
    return DelegatePackage.builder().delegateTask(delegateTask).build();
  }

  private DelegatePackage getDelegataePackageWithEncryptionConfig(DelegateTask delegateTask, String delegateId) {
    try {
      if (CapabilityHelper.isTaskParameterType(delegateTask.getData())) {
        return resolvePreAssignmentExpressions(delegateTask, delegateId);
      } else {
        // TODO: Ideally we should not land here, as we should always be passing TaskParameter only for
        // TODO: delegate task. But for now, this is needed. (e.g. Tasks containing Jenkinsonfig, BambooConfig etc.)
        Map<String, EncryptionConfig> encryptionConfigMap =
            CapabilityHelper.fetchEncryptionDetailsListFromParameters(delegateTask.getData());

        return DelegatePackage.builder().delegateTask(delegateTask).encryptionConfigs(encryptionConfigMap).build();
      }
    } catch (CriticalExpressionEvaluationException exception) {
      logger.error("Exception in ManagerPreExecutionExpressionEvaluator ", exception);
      Query<DelegateTask> taskQuery = wingsPersistence.createQuery(DelegateTask.class)
                                          .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                          .filter(ID_KEY, delegateTask.getUuid());
      DelegateTaskResponse response =
          DelegateTaskResponse.builder()
              .response(ErrorNotifyResponseData.builder().errorMessage(ExceptionUtils.getMessage(exception)).build())
              .responseCode(ResponseCode.FAILED)
              .accountId(delegateTask.getAccountId())
              .build();
      handleResponse(delegateTask.getUuid(), delegateId, delegateTask, taskQuery, response, ERROR);
      return null;
    }
  }

  private void handleResponse(String taskId, String delegateId, DelegateTask delegateTask,
      Query<DelegateTask> taskQuery, DelegateTaskResponse response, DelegateTask.Status error) {
    if (delegateTask.isAsync()) {
      String waitId = delegateTask.getWaitId();
      if (waitId != null) {
        waitNotifyEngine.notify(waitId, response.getResponse());
      } else {
        logger.error("Async task {} has no wait ID", taskId);
      }
      wingsPersistence.delete(taskQuery);
    } else {
      wingsPersistence.update(taskQuery,
          wingsPersistence.createUpdateOperations(DelegateTask.class)
              .set(DelegateTaskKeys.notifyResponse, KryoUtils.asBytes(response.getResponse()))
              .set(DelegateTaskKeys.status, error));
    }
  }

  private DelegatePackage assignTask(String delegateId, String taskId, DelegateTask delegateTask) {
    // Clear pending validations. No longer need to track since we're assigning.
    clearFromValidationCache(delegateTask);

    logger.info("Assigning {} task {} to delegate {} {}", delegateTask.getData().getTaskType(), taskId, delegateId,
        delegateTask.isAsync() ? "(async)" : "(sync)");
    Query<DelegateTask> query = wingsPersistence.createQuery(DelegateTask.class)
                                    .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                    .filter(DelegateTaskKeys.status, QUEUED)
                                    .field(DelegateTaskKeys.delegateId)
                                    .doesNotExist()
                                    .filter(ID_KEY, taskId)
                                    .project(DelegateTaskKeys.data_parameters, false);
    UpdateOperations<DelegateTask> updateOperations = wingsPersistence.createUpdateOperations(DelegateTask.class)
                                                          .set(DelegateTaskKeys.delegateId, delegateId)
                                                          .set(DelegateTaskKeys.status, STARTED);
    DelegateTask task =
        wingsPersistence.findAndModifySystemData(query, updateOperations, HPersistence.returnNewOptions);
    // If the task wasn't updated because delegateId already exists then query for the task with the delegateId in case
    // client is retrying the request
    if (task != null) {
      logger.info("Task {} assigned to delegate {}", taskId, delegateId);
      task.getData().setParameters(delegateTask.getData().getParameters());
      return resolvePreAssignmentExpressions(task, delegateId);
    }
    task = wingsPersistence.createQuery(DelegateTask.class)
               .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
               .filter(DelegateTaskKeys.status, STARTED)
               .filter(DelegateTaskKeys.delegateId, delegateId)
               .filter(ID_KEY, taskId)
               .project(DelegateTaskKeys.data_parameters, false)
               .get();
    if (task == null) {
      logger.info("Task {} no longer available for delegate {}", taskId, delegateId);
      return null;
    }

    task.getData().setParameters(delegateTask.getData().getParameters());
    logger.info("Returning previously assigned task {} to delegate {}", taskId, delegateId);
    return resolvePreAssignmentExpressions(task, delegateId);
  }

  @Override
  public void clearCache(String accountId, String delegateId) {
    assignDelegateService.clearConnectionResults(accountId, delegateId);
  }

  @Override
  public void processDelegateResponse(
      String accountId, String delegateId, String taskId, DelegateTaskResponse response) {
    if (response == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, NOBODY).addParam("args", "response cannot be null");
    }

    logger.info("Delegate [{}], response received for taskId [{}], responseCode [{}]", delegateId, taskId,
        response.getResponseCode());

    Query<DelegateTask> taskQuery = wingsPersistence.createQuery(DelegateTask.class)
                                        .filter(DelegateTaskKeys.accountId, response.getAccountId())
                                        .filter(ID_KEY, taskId);

    DelegateTask delegateTask = taskQuery.get();

    if (delegateTask != null) {
      if (!StringUtils.equals(delegateTask.getVersion(), getVersion())) {
        logger.warn("Version mismatch for task {} in account {}. [managerVersion {}, taskVersion {}]",
            delegateTask.getUuid(), delegateTask.getAccountId(), getVersion(), delegateTask.getVersion());
      }

      if (response.getResponseCode() == ResponseCode.RETRY_ON_OTHER_DELEGATE) {
        logger.info("Delegate {} returned retryable error for task {}.", delegateId, taskId);

        Set<String> alreadyTriedDelegates = delegateTask.getAlreadyTriedDelegates();
        List<String> remainingConnectedDelegates =
            assignDelegateService.connectedWhitelistedDelegates(delegateTask)
                .stream()
                .filter(item -> !delegateId.equals(item))
                .filter(item -> isEmpty(alreadyTriedDelegates) || !alreadyTriedDelegates.contains(item))
                .collect(toList());

        if (!remainingConnectedDelegates.isEmpty()) {
          logger.info("Requeueing task {}.", taskId);

          wingsPersistence.update(taskQuery,
              wingsPersistence.createUpdateOperations(DelegateTask.class)
                  .unset(DelegateTaskKeys.delegateId)
                  .unset(DelegateTaskKeys.validationStartedAt)
                  .unset(DelegateTaskKeys.lastBroadcastAt)
                  .unset(DelegateTaskKeys.validatingDelegateIds)
                  .unset(DelegateTaskKeys.validationCompleteDelegateIds)
                  .set(DelegateTaskKeys.broadcastCount, 1)
                  .set(DelegateTaskKeys.status, QUEUED)
                  .addToSet(DelegateTaskKeys.alreadyTriedDelegates, delegateId));
          return;
        } else {
          logger.info("Task {} has been tried on all the connected delegates. Proceeding with error.", taskId);
        }
      }

      handleResponse(taskId, delegateId, delegateTask, taskQuery, response, FINISHED);
      assignDelegateService.refreshWhitelist(delegateTask, delegateId);
    } else {
      logger.warn("No delegate task found: {}", taskId);
    }
  }

  @Override
  public boolean filter(String delegateId, DelegateTask task) {
    Delegate delegate = get(task.getAccountId(), delegateId, false);
    return delegate != null && StringUtils.equals(delegate.getAccountId(), task.getAccountId());
  }

  @Override
  public boolean filter(String delegateId, DelegateTaskAbortEvent taskAbortEvent) {
    return wingsPersistence.createQuery(DelegateTask.class)
               .filter(ID_KEY, taskAbortEvent.getDelegateTaskId())
               .filter(DelegateTaskKeys.delegateId, delegateId)
               .filter(DelegateTaskKeys.accountId, taskAbortEvent.getAccountId())
               .getKey()
        != null;
  }

  @Override
  public void expireTask(String accountId, String delegateTaskId) {
    if (delegateTaskId == null) {
      logger.warn("Delegate task id was null", new IllegalArgumentException());
      return;
    }
    logger.info("Expiring delegate task {}", delegateTaskId);
    Query<DelegateTask> delegateTaskQuery = getRunningTaskQuery(accountId, delegateTaskId);

    DelegateTask delegateTask = delegateTaskQuery.get();

    if (delegateTask != null) {
      String errorMessage = assignDelegateService.getActiveDelegateAssignmentErrorMessage(delegateTask);
      logger.info("Marking task as expired - {}: {}", delegateTask.getUuid(), errorMessage);

      if (isNotBlank(delegateTask.getWaitId())) {
        waitNotifyEngine.notify(
            delegateTask.getWaitId(), ErrorNotifyResponseData.builder().errorMessage(errorMessage).build());
      }
    }

    endTask(accountId, delegateTaskId, delegateTaskQuery, ERROR);
  }

  @Override
  public void abortTask(String accountId, String delegateTaskId) {
    if (delegateTaskId == null) {
      logger.warn("Delegate task id was null", new IllegalArgumentException());
      return;
    }
    logger.info("Aborting delegate task {}", delegateTaskId);
    endTask(accountId, delegateTaskId, getRunningTaskQuery(accountId, delegateTaskId), ABORTED);
  }

  private void endTask(
      String accountId, String delegateTaskId, Query<DelegateTask> delegateTaskQuery, DelegateTask.Status error) {
    wingsPersistence.update(delegateTaskQuery,
        wingsPersistence.createUpdateOperations(DelegateTask.class).set(DelegateTaskKeys.status, error));

    broadcasterFactory.lookup("/stream/delegate/" + accountId, true)
        .broadcast(aDelegateTaskAbortEvent().withAccountId(accountId).withDelegateTaskId(delegateTaskId).build());
  }

  private Query<DelegateTask> getRunningTaskQuery(String accountId, String delegateTaskId) {
    Query<DelegateTask> delegateTaskQuery = wingsPersistence.createQuery(DelegateTask.class)
                                                .filter(ID_KEY, delegateTaskId)
                                                .filter(DelegateTaskKeys.accountId, accountId)
                                                .filter(DelegateTaskKeys.async, true);
    delegateTaskQuery.or(delegateTaskQuery.criteria(DelegateTaskKeys.status).equal(QUEUED),
        delegateTaskQuery.criteria(DelegateTaskKeys.status).equal(STARTED));
    return delegateTaskQuery;
  }

  @Override
  public List<DelegateTaskEvent> getDelegateTaskEvents(String accountId, String delegateId, boolean syncOnly) {
    List<DelegateTaskEvent> delegateTaskEvents = new ArrayList<>(getQueuedEvents(accountId, true));
    if (!syncOnly) {
      delegateTaskEvents.addAll(getQueuedEvents(accountId, false));
      delegateTaskEvents.addAll(getAbortedEvents(accountId, delegateId));
    }

    logger.info("Dispatched delegateTaskIds:{} to delegate:[{}]",
        Joiner.on(",").join(delegateTaskEvents.stream().map(DelegateTaskEvent::getDelegateTaskId).collect(toList())),
        delegateId);

    return delegateTaskEvents;
  }

  private List<DelegateTaskEvent> getQueuedEvents(String accountId, boolean sync) {
    Query<DelegateTask> delegateTaskQuery =
        wingsPersistence.createQuery(DelegateTask.class)
            .filter(DelegateTaskKeys.accountId, accountId)
            .filter(DelegateTaskKeys.version, versionInfoManager.getVersionInfo().getVersion())
            .filter(DelegateTaskKeys.status, QUEUED)
            .filter(DelegateTaskKeys.async, !sync)
            .field(DelegateTaskKeys.delegateId)
            .doesNotExist();

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
    Query<DelegateTask> abortedQuery = wingsPersistence.createQuery(DelegateTask.class)
                                           .filter(DelegateTaskKeys.status, ABORTED)
                                           .filter(DelegateTaskKeys.async, true)
                                           .filter(DelegateTaskKeys.accountId, accountId)
                                           .filter(DelegateTaskKeys.delegateId, delegateId);

    // Send abort event only once by clearing delegateId
    wingsPersistence.update(
        abortedQuery, wingsPersistence.createUpdateOperations(DelegateTask.class).unset(DelegateTaskKeys.delegateId));

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
    wingsPersistence.delete(wingsPersistence.createQuery(Delegate.class).filter(DelegateTaskKeys.accountId, accountId));
  }

  //------ Start: ECS Delegate Specific Methods

  /**
   * Delegate keepAlive and Registration requests will be handled here
   */
  @Override
  public Delegate handleEcsDelegateRequest(Delegate delegate) {
    if (delegate.isKeepAlivePacket()) {
      return handleEcsDelegateKeepAlivePacket(delegate);
    }
    Delegate registeredDelegate = handleEcsDelegateRegistration(delegate);
    updateExistingDelegateWithSequenceConfigData(registeredDelegate);
    return registeredDelegate;
  }

  /**
   * ECS delegate sends keepAlive request every 20 secs. KeepAlive request is a frequent and light weight
   * mode for indicating that delegate is active.
   *
   * We just update "lastUpdatedAt" field with latest time for DelegateSequenceConfig associated with delegate,
   * so we can found stale config (not updated in last 100 secs) when we need to reuse it for new delegate
   * registration.
   */
  @VisibleForTesting
  Delegate handleEcsDelegateKeepAlivePacket(Delegate delegate) {
    logger.info("Handling Keep alive packet ");
    if (isBlank(delegate.getHostName()) || isBlank(delegate.getDelegateRandomToken()) || isBlank(delegate.getUuid())
        || isBlank(delegate.getSequenceNum())) {
      return null;
    }

    Delegate existingDelegate =
        getDelegateUsingSequenceNum(delegate.getAccountId(), delegate.getHostName(), delegate.getSequenceNum());
    if (existingDelegate == null) {
      return null;
    }

    DelegateSequenceConfig config = getDelegateSequenceConfig(
        delegate.getAccountId(), delegate.getHostName(), Integer.parseInt(delegate.getSequenceNum()));

    if (config != null && config.getDelegateToken().equals(delegate.getDelegateRandomToken())) {
      Query<DelegateSequenceConfig> sequenceConfigQuery =
          wingsPersistence.createQuery(DelegateSequenceConfig.class).filter(ID_KEY, config.getUuid());
      wingsPersistence.update(sequenceConfigQuery,
          wingsPersistence.createUpdateOperations(DelegateSequenceConfig.class)
              .set(DelegateSequenceConfigKeys.delegateToken, delegate.getDelegateRandomToken()));
    }

    return null;
  }

  /**
   * Handles first time registration or heartbeat request send by delegate
   */
  @VisibleForTesting
  Delegate handleEcsDelegateRegistration(Delegate delegate) {
    // SCENARIO 1: Received delegateId with the request and delegate exists in DB.
    // Just update same existing delegate

    if (delegate.getUuid() != null && isValidSeqNum(delegate.getSequenceNum())
        && checkForValidTokenIfPresent(delegate)) {
      Delegate registeredDelegate = handleECSRegistrationUsingID(delegate);
      if (registeredDelegate != null) {
        return registeredDelegate;
      }
    }

    // can not proceed unless we receive valid token
    if (isBlank(delegate.getDelegateRandomToken()) || "null".equalsIgnoreCase(delegate.getDelegateRandomToken())) {
      throw new WingsException(GENERAL_ERROR, "Received invalid token from ECS delegate", USER_SRE)
          .addParam("message", "Received invalid token from ECS delegate");
    }

    // SCENARIO 2: Delegate passed sequenceNum & delegateToken but not UUID.
    // So delegate was registered earlier but may be got restarted and trying re-register.
    if (isValidSeqNum(delegate.getSequenceNum()) && isNotBlank(delegate.getDelegateRandomToken())) {
      Delegate registeredDelegate = handleECSRegistrationUsingSeqNumAndToken(delegate);
      if (registeredDelegate != null) {
        return registeredDelegate;
      }
    }

    // SCENARIO 3: Create new SequenceNum for delegate.
    // We will reach here in 2 scenarios,
    // 1. Delegate did not pass any sequenceNum or delegateToken. (This is first time delegate is registering after
    // start up or disk file delegate writes to, got deleted).

    // 2. Delegate passed seqNum & delegateToken, but We got DuplicateKeyException in SCENARIO 2
    // In any of these cases, it will be treated as fresh registration and new sequenceNum will be generated.
    return registerDelegateWithNewSequenceGeneration(delegate);
  }

  @VisibleForTesting
  boolean checkForValidTokenIfPresent(Delegate delegate) {
    DelegateSequenceConfig config = getDelegateSequenceConfig(
        delegate.getAccountId(), delegate.getHostName(), Integer.parseInt(delegate.getSequenceNum()));
    return config != null && config.getDelegateToken().equals(delegate.getDelegateRandomToken());
  }

  /**
   * Delegate sent token and seqNum but null UUID.
   * 1. See if DelegateSequenceConfig record with same {accId, SeqNum} has same token as passed by delegate.
   *    If yes,
   *       - get delegate associated with this DelegateSequenceConfig if exists and update it.
   *       - if delegate does not present in db, create a new record (init it with config from similar delegate and
   * create record)
   *
   *    IF No,
   *      - Means that seqNum has been acquired by another delegate.
   *      - Generate a new SeqNum and create delegate record using it (init it with config from similar delegate and
   * create record).
   */
  @VisibleForTesting
  Delegate handleECSRegistrationUsingSeqNumAndToken(Delegate delegate) {
    logger.info("Delegate sent seqNum : " + delegate.getSequenceNum() + ", and DelegateToken"
        + delegate.getDelegateRandomToken());

    DelegateSequenceConfig sequenceConfig = getDelegateSequenceConfig(
        delegate.getAccountId(), delegate.getHostName(), Integer.parseInt(delegate.getSequenceNum()));

    Delegate existingDelegate = null;
    boolean delegateConfigMatches = false;
    // SequenceConfig found with same {HostName, AccountId, SequenceNum, DelegateToken}.
    // Its same delegate sending request with valid data. Find actual delegate record using this
    // DelegateSequenceConfig
    if (seqNumAndTokenMatchesConfig(delegate, sequenceConfig)) {
      delegateConfigMatches = true;
      existingDelegate = getDelegateUsingSequenceNum(
          sequenceConfig.getAccountId(), sequenceConfig.getHostName(), sequenceConfig.getSequenceNum().toString());
    }

    // No Existing delegate was found, so create new delegate record on manager side,
    // using {seqNum, delegateToken} passed by delegate.
    if (existingDelegate == null) {
      try {
        DelegateSequenceConfig config = delegateConfigMatches
            ? sequenceConfig
            : generateNewSeqenceConfig(delegate, Integer.parseInt(delegate.getSequenceNum()));

        String hostNameWithSeqNum =
            getHostNameToBeUsedForECSDelegate(config.getHostName(), config.getSequenceNum().toString());
        delegate.setHostName(hostNameWithSeqNum);

        // Init this delegate with {TAG/SCOPE/PROFILE} config reading from similar delegate
        initDelegateWithConfigFromExistingDelegate(delegate);

        return upsertDelegateOperation(null, delegate);
      } catch (DuplicateKeyException e) {
        logger.warn(
            "SequenceNum passed by delegate has been assigned to a new delegate. will regenerate new sequenceNum.");
      }
    } else {
      // Existing delegate was found, so just update it.
      return upsertDelegateOperation(existingDelegate, delegate);
    }

    return null;
  }

  @VisibleForTesting
  boolean seqNumAndTokenMatchesConfig(Delegate delegate, DelegateSequenceConfig sequenceConfig) {
    return sequenceConfig != null && sequenceConfig.getSequenceNum() != null
        && isNotBlank(sequenceConfig.getDelegateToken())
        && sequenceConfig.getDelegateToken().equals(delegate.getDelegateRandomToken())
        && sequenceConfig.getSequenceNum().toString().equals(delegate.getSequenceNum());
  }

  /**
   * Get Delegate associated with {AccountId, HostName, SeqNum}
   */
  @VisibleForTesting
  Delegate getDelegateUsingSequenceNum(String accountId, String hostName, String seqNum) {
    Delegate existingDelegate;
    Query<Delegate> delegateQuery =
        wingsPersistence.createQuery(Delegate.class)
            .filter(DelegateKeys.accountId, accountId)
            .filter(DelegateSequenceConfigKeys.hostName, getHostNameToBeUsedForECSDelegate(hostName, seqNum));

    existingDelegate = delegateQuery.get();
    return existingDelegate;
  }

  /**
   * Get existing delegate having same {hostName (prefix without seqNum), AccId, type = ECS}
   * Copy {SCOPE/PROFILE/TAG} config into new delegate being registered
   *
   */
  @VisibleForTesting
  void initDelegateWithConfigFromExistingDelegate(Delegate delegate) {
    List<Delegate> existingDelegates = getAllDelegatesMatchingGroupName(delegate);
    if (isNotEmpty(existingDelegates)) {
      initNewDelegateWithExistingDelegate(delegate, existingDelegates.get(0));
    }
  }

  /**
   * Delegate send UUID, if record exists, just update same one.
   */
  @VisibleForTesting
  Delegate handleECSRegistrationUsingID(Delegate delegate) {
    Query<Delegate> delegateQuery =
        wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.uuid, delegate.getUuid());

    Delegate existingDelegate = delegateQuery.project(DelegateKeys.hostName, true)
                                    .project(DelegateKeys.status, true)
                                    .project(DelegateKeys.delegateProfileId, true)
                                    .get();

    if (existingDelegate != null) {
      return upsertDelegateOperation(existingDelegate, delegate);
    }

    return null;
  }

  /**
   * Either
   * 1. find a stale DelegateSeqConfig (not updated for last 100 secs),
   *    delete delegate associated with it and use this seqNum for new delegate registration.
   *
   * 2. Else no such config exists from point 1, Create new SequenceConfig and associate with delegate.
   * (In both cases, we copy config {SCOPE/TAG/PROFILE} from existing delegates to this new delegate being registered)
   */
  @VisibleForTesting
  Delegate registerDelegateWithNewSequenceGeneration(Delegate delegate) {
    List<DelegateSequenceConfig> existingDelegateSequenceConfigs = getDelegateSequenceConfigs(delegate);

    // Find Inactive DelegateSequenceConfig with same Acc and hostName and delete associated delegate
    DelegateSequenceConfig config =
        getInactiveDelegateSequenceConfigToReplace(delegate, existingDelegateSequenceConfigs);

    if (config != null) {
      return upsertDelegateOperation(null, delegate);
    }

    // Could not find InactiveDelegateConfig, Create new SequenceConfig
    for (int i = 0; i < 3; i++) {
      try {
        config = addNewDelegateSequenceConfigRecord(delegate);
        String hostNameWithSeqNum =
            getHostNameToBeUsedForECSDelegate(delegate.getHostName(), config.getSequenceNum().toString());
        delegate.setHostName(hostNameWithSeqNum);

        // Init this delegate with TAG/SCOPE/PROFILE config reading from similar delegate
        initDelegateWithConfigFromExistingDelegate(delegate);

        return upsertDelegateOperation(null, delegate);
      } catch (Exception e) {
        logger.warn("Attempt: " + i + " failed with DuplicateKeyException. Trying again" + e);
      }
    }
    // All 3 attempts of sequenceNum generation for delegate failed. Registration can not be completed.
    // Delegate will need to send request again
    throw new WingsException(GENERAL_ERROR, "Failed to generate sequence number for Delegate", USER_SRE)
        .addParam("message", "Failed to generate sequence number for Delegate");
  }

  /**
   * This method expects, you have already stripped off seqNum for delegate host name
   */
  @VisibleForTesting
  List<DelegateSequenceConfig> getDelegateSequenceConfigs(Delegate delegate) {
    Query<DelegateSequenceConfig> delegateSequenceConfigQuery =
        wingsPersistence.createQuery(DelegateSequenceConfig.class)
            .filter(DelegateSequenceConfigKeys.accountId, delegate.getAccountId())
            .filter(DelegateSequenceConfigKeys.hostName, delegate.getHostName());

    return delegateSequenceConfigQuery.project(ID_KEY, true)
        .project(DelegateSequenceConfigKeys.sequenceNum, true)
        .project(DelegateSequenceConfig.LAST_UPDATED_AT_KEY, true)
        .project(DelegateSequenceConfig.ACCOUNT_ID_KEY, true)
        .project(DelegateSequenceConfigKeys.hostName, true)
        .project(DelegateSequenceConfigKeys.delegateToken, true)
        .asList();
  }

  @VisibleForTesting
  DelegateSequenceConfig addNewDelegateSequenceConfigRecord(Delegate delegate) {
    Query<DelegateSequenceConfig> delegateSequenceConfigQuery =
        wingsPersistence.createQuery(DelegateSequenceConfig.class)
            .filter(DelegateSequenceConfig.ACCOUNT_ID_KEY, delegate.getAccountId())
            .filter(DelegateSequenceConfigKeys.hostName, delegate.getHostName());

    List<DelegateSequenceConfig> existingDelegateSequenceConfigs =
        delegateSequenceConfigQuery.project(DelegateSequenceConfigKeys.sequenceNum, true)
            .project(DelegateSequenceConfig.LAST_UPDATED_AT_KEY, true)
            .project(DelegateSequenceConfig.ACCOUNT_ID_KEY, true)
            .project(DelegateSequenceConfigKeys.hostName, true)
            .project(DelegateSequenceConfigKeys.delegateToken, true)
            .asList();

    existingDelegateSequenceConfigs = existingDelegateSequenceConfigs.stream()
                                          .sorted(comparingInt(DelegateSequenceConfig::getSequenceNum))
                                          .collect(toList());

    int num = 0;
    for (DelegateSequenceConfig existingDelegateSequenceConfig : existingDelegateSequenceConfigs) {
      if (num < existingDelegateSequenceConfig.getSequenceNum()) {
        break;
      }
      num++;
    }

    delegate.setSequenceNum(String.valueOf(num));
    return generateNewSeqenceConfig(delegate, num);
  }

  @VisibleForTesting
  DelegateSequenceConfig getInactiveDelegateSequenceConfigToReplace(
      Delegate delegate, List<DelegateSequenceConfig> existingDelegateSequenceConfigs) {
    DelegateSequenceConfig config;
    try {
      Optional<DelegateSequenceConfig> optionalConfig =
          existingDelegateSequenceConfigs.stream()
              .filter(sequenceConfig
                  -> sequenceConfig.getLastUpdatedAt() < System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(100))
              .findFirst();

      if (optionalConfig.isPresent()) {
        config = optionalConfig.get();

        Delegate existingInactiveDelegate = getDelegateUsingSequenceNum(
            delegate.getAccountId(), config.getHostName(), config.getSequenceNum().toString());

        if (existingInactiveDelegate != null) {
          // Before deleting existing one, copy {TAG/PROFILE/SCOPE} config into new delegate being registered
          // This needs to be done here as this may be the only delegate in db.
          initNewDelegateWithExistingDelegate(delegate, existingInactiveDelegate);
          delete(existingInactiveDelegate.getAccountId(), existingInactiveDelegate.getUuid());
        }

        Query<DelegateSequenceConfig> sequenceConfigQuery =
            wingsPersistence.createQuery(DelegateSequenceConfig.class).filter("_id", config.getUuid());
        wingsPersistence.update(sequenceConfigQuery,
            wingsPersistence.createUpdateOperations(DelegateSequenceConfig.class)
                .set(DelegateSequenceConfigKeys.delegateToken, delegate.getDelegateRandomToken()));

        // Update delegate with seqNum and hostName
        delegate.setSequenceNum(config.getSequenceNum().toString());
        String hostNameWithSeqNum =
            getHostNameToBeUsedForECSDelegate(config.getHostName(), config.getSequenceNum().toString());
        delegate.setHostName(hostNameWithSeqNum);

        if (existingInactiveDelegate == null) {
          initDelegateWithConfigFromExistingDelegate(delegate);
        }
        return config;
      }
    } catch (Exception e) {
      logger.warn("Failed while updating delegateSequenceConfig with delegateToken: {}, DelegateId: {}",
          delegate.getDelegateRandomToken(), delegate.getUuid());
    }

    return null;
  }

  private DelegateSequenceConfig generateNewSeqenceConfig(Delegate delegate, Integer seqNum) {
    logger.info("Adding delegateSequenceConfig For delegate.hostname: {}, With SequenceNum: {}, for account:  {}",
        delegate.getHostName(), delegate.getSequenceNum(), delegate.getAccountId());

    DelegateSequenceConfig sequenceConfig = aDelegateSequenceBuilder()
                                                .withSequenceNum(seqNum)
                                                .withAccountId(delegate.getAccountId())
                                                .withHostName(delegate.getHostName())
                                                .withDelegateToken(delegate.getDelegateRandomToken())
                                                .withAppId(GLOBAL_APP_ID)
                                                .build();

    wingsPersistence.save(sequenceConfig);
    logger.info("DelegateSequenceConfig saved: {}", sequenceConfig);

    return sequenceConfig;
  }

  private String getHostNameToBeUsedForECSDelegate(String hostName, String seqNum) {
    return hostName + DELIMITER + seqNum;
  }

  /**
   * Copy {SCOPE/TAG/PROFILE/KEYWORDS} into new delegate
   */
  private void initNewDelegateWithExistingDelegate(Delegate delegate, Delegate existingInactiveDelegate) {
    delegate.setExcludeScopes(existingInactiveDelegate.getExcludeScopes());
    delegate.setIncludeScopes(existingInactiveDelegate.getIncludeScopes());
    delegate.setDelegateProfileId(existingInactiveDelegate.getDelegateProfileId());
    delegate.setTags(existingInactiveDelegate.getTags());
    delegate.setKeywords(existingInactiveDelegate.getKeywords());
  }

  private Delegate updateAllDelegatesIfECSType(
      Delegate delegate, UpdateOperations<Delegate> updateOperations, String fieldBeingUpdate) {
    final List<Delegate> retVal = new ArrayList<>();
    List<Delegate> delegates = getAllDelegatesMatchingGroupName(delegate);

    if (isEmpty(delegates)) {
      return null;
    }

    delegates.forEach(delegateToBeUpdated -> {
      // LOGGING logic
      if ("SCOPES".equals(fieldBeingUpdate)) {
        logger.info("Updating delegate scopes : Delegate:{} includeScopes:{} excludeScopes:{}",
            delegateToBeUpdated.getUuid(), delegate.getIncludeScopes(), delegate.getExcludeScopes());
      } else if ("TAGS".equals(fieldBeingUpdate)) {
        logger.info("Updating delegate tags : Delegate:{} tags:{}", delegateToBeUpdated.getUuid(), delegate.getTags());
      } else {
        logger.info("Updating delegate : {}", delegateToBeUpdated.getUuid());
      }

      Delegate updatedDelegate = updateDelegate(delegateToBeUpdated, updateOperations);
      if (updatedDelegate.getUuid().equals(delegate.getUuid())) {
        retVal.add(updatedDelegate);
      }
      if (System.currentTimeMillis() - updatedDelegate.getLastHeartBeat() < 2 * 60 * 1000) {
        alertService.activeDelegateUpdated(updatedDelegate.getAccountId(), updatedDelegate.getUuid());
      }
    });

    if (isNotEmpty(retVal)) {
      return retVal.get(0);
    } else {
      return null;
    }
  }

  /**
   * All delegates matching {AccId, HostName Prefix, Type = ECS}
   */
  private List<Delegate> getAllDelegatesMatchingGroupName(Delegate delegate) {
    return wingsPersistence.createQuery(Delegate.class, excludeAuthority)
        .filter(DelegateKeys.accountId, delegate.getAccountId())
        .filter(DelegateKeys.delegateType, delegate.getDelegateType())
        .filter(DelegateKeys.delegateGroupName, delegate.getDelegateGroupName())
        .asList();
  }

  private boolean isValidSeqNum(String sequenceNum) {
    try {
      Integer.parseInt(sequenceNum);
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  private boolean isDelegateWithoutPollingEnabled(Delegate delegate) {
    return !delegate.isPolllingModeEnabled();
  }

  private void updateWithTokenAndSeqNumIfEcsDelegate(Delegate delegate, Delegate savedDelegate) {
    if (ECS.equals(delegate.getDelegateType())) {
      savedDelegate.setDelegateRandomToken(delegate.getDelegateRandomToken());
      savedDelegate.setSequenceNum(delegate.getSequenceNum());
    }
  }

  @VisibleForTesting
  void updateExistingDelegateWithSequenceConfigData(Delegate delegate) {
    String hostName = getDelegateHostNameByRemovingSeqNum(delegate);
    String seqNum = getDelegateSeqNumFromHostName(delegate);
    DelegateSequenceConfig config =
        getDelegateSequenceConfig(delegate.getAccountId(), hostName, Integer.parseInt(seqNum));
    delegate.setDelegateRandomToken(config.getDelegateToken());
    delegate.setSequenceNum(String.valueOf(config.getSequenceNum()));
  }

  @VisibleForTesting
  String getDelegateHostNameByRemovingSeqNum(Delegate delegate) {
    return delegate.getHostName().substring(0, delegate.getHostName().lastIndexOf('_'));
  }

  @VisibleForTesting
  String getDelegateSeqNumFromHostName(Delegate delegate) {
    return delegate.getHostName().substring(delegate.getHostName().lastIndexOf('_') + 1);
  }
  //------ END: ECS Delegate Specific Methods
}
