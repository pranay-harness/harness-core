package software.wings.service.impl;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Sets.newHashSet;
import static freemarker.template.Configuration.VERSION_2_3_23;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.eraro.ErrorCode.UNAVAILABLE_DELEGATES;
import static io.harness.exception.WingsException.NOBODY;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.beans.Delegate.HOST_NAME_KEY;
import static software.wings.beans.DelegateConnection.defaultExpiryTimeInMinutes;
import static software.wings.beans.DelegateTask.Status.ABORTED;
import static software.wings.beans.DelegateTask.Status.ERROR;
import static software.wings.beans.DelegateTask.Status.FINISHED;
import static software.wings.beans.DelegateTask.Status.QUEUED;
import static software.wings.beans.DelegateTask.Status.STARTED;
import static software.wings.beans.DelegateTaskAbortEvent.Builder.aDelegateTaskAbortEvent;
import static software.wings.beans.DelegateTaskEvent.DelegateTaskEventBuilder.aDelegateTaskEvent;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.FeatureName.DELEGATE_TASK_VERSIONING;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.ServiceSecretKey.ServiceType.LEARNING_ENGINE;
import static software.wings.beans.alert.AlertType.NoEligibleDelegates;
import static software.wings.beans.alert.NoEligibleDelegatesAlert.NoEligibleDelegatesAlertBuilder.aNoEligibleDelegatesAlert;
import static software.wings.common.Constants.DELEGATE_DIR;
import static software.wings.common.Constants.DOCKER_DELEGATE;
import static software.wings.common.Constants.KUBERNETES_DELEGATE;
import static software.wings.common.Constants.MAX_DELEGATE_LAST_HEARTBEAT;
import static software.wings.common.Constants.SELF_DESTRUCT;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.ALL_DELEGATE_DOWN_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.DELEGATE_STATE_NOTIFICATION;
import static software.wings.delegatetasks.RemoteMethodReturnValueData.Builder.aRemoteMethodReturnValueData;
import static software.wings.utils.KubernetesConvention.getAccountIdentifier;

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

import com.github.zafarkhaja.semver.Version;
import com.mongodb.MongoGridFSException;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.persistence.ReadPref;
import io.harness.version.VersionInfoManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atteo.evo.inflector.English;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.DeployMode;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateConfiguration;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateConnectionHeartbeat;
import software.wings.beans.DelegateProfile;
import software.wings.beans.DelegateProfileParams;
import software.wings.beans.DelegateScripts;
import software.wings.beans.DelegateStatus;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.DelegateTaskResponse.ResponseCode;
import software.wings.beans.Event.Type;
import software.wings.beans.FileMetadata;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DelegateProfileErrorAlert;
import software.wings.beans.alert.DelegatesDownAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.WingsPersistence;
import software.wings.expression.SecretFunctor;
import software.wings.licensing.LicenseService;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.impl.infra.InfraDownloadService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.DelegateMetaInfo;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.KryoUtils;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
public class DelegateServiceImpl implements DelegateService, Runnable {
  private static final Logger logger = LoggerFactory.getLogger(DelegateServiceImpl.class);

  private static final String ACCOUNT_ID = "accountId";
  private static final Configuration cfg = new Configuration(VERSION_2_3_23);
  private static final int MAX_DELEGATE_META_INFO_ENTRIES = 10000;
  private static final int DELEGATE_METADATA_HTTP_CALL_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);
  private static final Set<DelegateTask.Status> TASK_COMPLETED_STATUSES = ImmutableSet.of(FINISHED, ABORTED, ERROR);

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
  @Inject private NotificationService notificationService;
  @Inject private NotificationSetupService notificationSetupService;
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

  private final Map<String, Object> syncTaskWaitMap = new ConcurrentHashMap<>();

  private LoadingCache<String, String> delegateVersionCache =
      CacheBuilder.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(1, TimeUnit.MINUTES)
          .build(new CacheLoader<String, String>() {
            public String load(String accountId) {
              return fetchAccountDelegateMetadataFromStorage(accountId);
            }
          });

  private LoadingCache<String, Optional<Delegate>> delegateCache =
      CacheBuilder.newBuilder()
          .maximumSize(MAX_DELEGATE_META_INFO_ENTRIES)
          .expireAfterWrite(1, TimeUnit.MINUTES)
          .build(new CacheLoader<String, Optional<Delegate>>() {
            public Optional<Delegate> load(String delegateId) throws NotFoundException {
              return Optional.ofNullable(wingsPersistence.createQuery(Delegate.class).filter(ID_KEY, delegateId).get());
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
                                              .filter("async", false)
                                              .field("status")
                                              .in(TASK_COMPLETED_STATUSES)
                                              .field(ID_KEY)
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
        .filter("accountId", accountId)
        .field("delegateName")
        .exists()
        .project("delegateName", true)
        .asList()
        .stream()
        .map(Delegate::getDelegateName)
        .distinct()
        .sorted(naturalOrder())
        .collect(toList());
  }

  @Override
  public Set<String> getAllDelegateTags(String accountId) {
    List<Delegate> delegates =
        wingsPersistence.createQuery(Delegate.class).filter("accountId", accountId).field("tags").exists().asList();
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
    List<Delegate> delegates = wingsPersistence.createQuery(Delegate.class).filter("accountId", accountId).asList();
    List<DelegateConnection> delegateConnections = wingsPersistence.createQuery(DelegateConnection.class)
                                                       .filter("accountId", accountId)
                                                       .project("delegateId", true)
                                                       .project("version", true)
                                                       .project("lastHeartbeat", true)
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
                                  .ip(delegate.getIp())
                                  .status(delegate.getStatus())
                                  .lastHeartBeat(delegate.getLastHeartBeat())
                                  .delegateProfileId(delegate.getDelegateProfileId())
                                  .excludeScopes(delegate.getExcludeScopes())
                                  .includeScopes(delegate.getIncludeScopes())
                                  .tags(delegate.getTags())
                                  .profileExecutedAt(delegate.getProfileExecutedAt())
                                  .profileError(delegate.isProfileError())
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
    UpdateOperations<Delegate> updateOperations = wingsPersistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, "ip", delegate.getIp());
    setUnset(updateOperations, "status", delegate.getStatus());
    setUnset(updateOperations, "lastHeartBeat", delegate.getLastHeartBeat());
    setUnset(updateOperations, "connected", delegate.isConnected());
    setUnset(updateOperations, "version", delegate.getVersion());
    setUnset(updateOperations, "description", delegate.getDescription());
    setUnset(updateOperations, "delegateProfileId", delegate.getDelegateProfileId());

    logger.info("Updating delegate : {}", delegate.getUuid());
    return updateDelegate(delegate, updateOperations);
  }

  @Override
  public Delegate updateDescription(String accountId, String delegateId, String newDescription) {
    logger.info("Updating delegate : {} with new description", delegateId);
    wingsPersistence.update(
        wingsPersistence.createQuery(Delegate.class).filter(ACCOUNT_ID, accountId).filter(ID_KEY, delegateId),
        wingsPersistence.createUpdateOperations(Delegate.class).set("description", newDescription));

    return get(accountId, delegateId, true);
  }

  @Override
  public Delegate updateHeartbeat(String accountId, String delegateId) {
    wingsPersistence.update(
        wingsPersistence.createQuery(Delegate.class).filter("accountId", accountId).filter(ID_KEY, delegateId),
        wingsPersistence.createUpdateOperations(Delegate.class)
            .set("lastHeartBeat", System.currentTimeMillis())
            .set("connected", true));

    Delegate delegate = get(accountId, delegateId, false);

    if (licenseService.isAccountDeleted(accountId)) {
      delegate.setStatus(Status.DELETED);
    }
    return delegate;
  }

  @Override
  public Delegate updateTags(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = wingsPersistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, "tags", delegate.getTags());
    logger.info("Updating delegate tags : Delegate:{} tags:{}", delegate.getUuid(), delegate.getTags());
    Delegate updatedDelegate = updateDelegate(delegate, updateOperations);
    if (System.currentTimeMillis() - updatedDelegate.getLastHeartBeat() < 2 * 60 * 1000) {
      alertService.activeDelegateUpdated(updatedDelegate.getAccountId(), updatedDelegate.getUuid());
    }
    return updatedDelegate;
  }

  @Override
  public Delegate updateScopes(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = wingsPersistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, "includeScopes", delegate.getIncludeScopes());
    setUnset(updateOperations, "excludeScopes", delegate.getExcludeScopes());

    logger.info("Updating delegate scopes : Delegate:{} includeScopes:{} excludeScopes:{}", delegate.getUuid(),
        delegate.getIncludeScopes(), delegate.getExcludeScopes());
    Delegate updatedDelegate = updateDelegate(delegate, updateOperations);
    if (System.currentTimeMillis() - updatedDelegate.getLastHeartBeat() < 2 * 60 * 1000) {
      alertService.activeDelegateUpdated(updatedDelegate.getAccountId(), updatedDelegate.getUuid());
    }
    return updatedDelegate;
  }

  private Delegate updateDelegate(Delegate delegate, UpdateOperations<Delegate> updateOperations) {
    Delegate previousDelegate = get(delegate.getAccountId(), delegate.getUuid(), false);

    if (previousDelegate != null && isBlank(delegate.getDelegateProfileId())) {
      updateOperations.unset("profileResult").unset("profileError").unset("profileExecutedAt");

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
                                .filter("accountId", delegate.getAccountId())
                                .filter(ID_KEY, delegate.getUuid()),
        updateOperations);

    // Touch currently executing tasks.
    if (delegate.getCurrentlyExecutingDelegateTasks() != null
        && isNotEmpty(delegate.getCurrentlyExecutingDelegateTasks())) {
      logger.info("Updating tasks");

      Query<DelegateTask> delegateTaskQuery =
          wingsPersistence.createQuery(DelegateTask.class)
              .filter("accountId", delegate.getAccountId())
              .filter("delegateId", delegate.getUuid())
              .filter("status", DelegateTask.Status.STARTED)
              .field("lastUpdatedAt")
              .lessThan(System.currentTimeMillis())
              .field(ID_KEY)
              .in(delegate.getCurrentlyExecutingDelegateTasks().stream().map(DelegateTask::getUuid).collect(toList()));
      wingsPersistence.update(delegateTaskQuery, wingsPersistence.createUpdateOperations(DelegateTask.class));
    }

    eventEmitter.send(Channel.DELEGATES,
        anEvent().withOrgId(delegate.getAccountId()).withUuid(delegate.getUuid()).withType(Type.UPDATE).build());
    return get(delegate.getAccountId(), delegate.getUuid(), true);
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
    }
    return delegateScripts;
  }

  public String getLatestDelegateVersion(String accountId) {
    String delegateMatadata = null;
    try {
      delegateMatadata = delegateVersionCache.get(accountId);
    } catch (ExecutionException e) {
      logger.error("Execution exception", e);
    }
    return substringBefore(delegateMatadata, " ").trim();
  }

  private String fetchAccountDelegateMetadataFromStorage(String acccountId) {
    // TODO:: Specific restriction for account can be handled here.
    String delegateMetadataUrl = mainConfiguration.getDelegateMetadataUrl().trim();
    try {
      logger.info("Fetching delegate metadata from storage: {}", delegateMetadataUrl);
      String result = Request.Get(delegateMetadataUrl)
                          .connectTimeout(DELEGATE_METADATA_HTTP_CALL_TIMEOUT)
                          .socketTimeout(DELEGATE_METADATA_HTTP_CALL_TIMEOUT)
                          .execute()
                          .returnContent()
                          .asString()
                          .trim();
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
        jarFileExists = Request.Head(delegateJarDownloadUrl)
                            .connectTimeout(10000)
                            .socketTimeout(10000)
                            .execute()
                            .handleResponse(response -> {
                              int statusCode = response.getStatusLine().getStatusCode();
                              logger.info("HEAD on downloadUrl got statusCode {}", statusCode);
                              return statusCode == 200;
                            });

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

      if (mainConfiguration.getDeployMode().equals(DeployMode.KUBERNETES_ONPREM)) {
        delegateDockerImage = mainConfiguration.getPortal().getDelegateDockerImage();
      }

      ImmutableMap.Builder<String, String> params = ImmutableMap.<String, String>builder()
                                                        .put("delegateDockerImage", delegateDockerImage)
                                                        .put("accountId", accountId)
                                                        .put("accountSecret", account.getAccountKey())
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

      File proxyConfig = File.createTempFile("proxy", ".config");
      try (BufferedWriter fileWriter =
               new BufferedWriter(new OutputStreamWriter(new FileOutputStream(proxyConfig), UTF_8))) {
        fileWriter.write("PROXY_HOST=");
        fileWriter.newLine();
        fileWriter.write("PROXY_PORT=");
        fileWriter.newLine();
        fileWriter.write("PROXY_SCHEME=");
        fileWriter.newLine();
        fileWriter.write("NO_PROXY=");
      }
      proxyConfig = new File(proxyConfig.getAbsolutePath());
      TarArchiveEntry proxyTarArchiveEntry = new TarArchiveEntry(proxyConfig, DELEGATE_DIR + "/proxy.config");
      out.putArchiveEntry(proxyTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(proxyConfig)) {
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
  public Delegate add(Delegate delegate) {
    logger.info("Adding delegate {} for account {}", delegate.getHostName(), delegate.getAccountId());
    delegate.setAppId(GLOBAL_APP_ID);
    Delegate savedDelegate = wingsPersistence.saveAndGet(Delegate.class, delegate);
    logger.info("Delegate saved: {}", savedDelegate.getUuid());
    eventEmitter.send(Channel.DELEGATES,
        anEvent().withOrgId(delegate.getAccountId()).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
    assignDelegateService.clearConnectionResults(delegate.getAccountId());
    return savedDelegate;
  }

  @Override
  public void delete(String accountId, String delegateId) {
    logger.info("Deleting delegate: {}", delegateId);
    Delegate existingDelegate = wingsPersistence.createQuery(Delegate.class)
                                    .filter("accountId", accountId)
                                    .filter(ID_KEY, delegateId)
                                    .project("ip", true)
                                    .project(HOST_NAME_KEY, true)
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

    wingsPersistence.delete(
        wingsPersistence.createQuery(Delegate.class).filter("accountId", accountId).filter(ID_KEY, delegateId));
  }

  @Override
  public Delegate register(Delegate delegate) {
    if (licenseService.isAccountDeleted(delegate.getAccountId())) {
      broadcasterFactory.lookup("/stream/delegate/" + delegate.getAccountId(), true).broadcast(SELF_DESTRUCT);
      return aDelegate().withUuid(SELF_DESTRUCT).build();
    }

    logger.info("Registering delegate for account {}: Hostname: {} IP: {}", delegate.getAccountId(),
        delegate.getHostName(), delegate.getIp());
    Query<Delegate> delegateQuery = wingsPersistence.createQuery(Delegate.class)
                                        .filter("accountId", delegate.getAccountId())
                                        .filter("hostName", delegate.getHostName());
    // For delegates running in a kubernetes cluster we include lowercase account ID in the hostname to identify it.
    // We ignore IP address because that can change with every restart of the pod.
    if (!delegate.getHostName().contains(getAccountIdentifier(delegate.getAccountId()))) {
      delegateQuery.filter("ip", delegate.getIp());
    }

    Delegate existingDelegate = delegateQuery.project("status", true).project("delegateProfileId", true).get();
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
      registeredDelegate = update(delegate);

      broadcasterFactory.lookup("/stream/delegate/" + delegate.getAccountId(), true)
          .broadcast("[X]" + delegate.getUuid());
    }
    alertService.activeDelegateUpdated(registeredDelegate.getAccountId(), registeredDelegate.getUuid());
    registeredDelegate.setVerificationServiceSecret(learningEngineService.getServiceSecretKey(LEARNING_ENGINE));
    return registeredDelegate;
  }

  @Override
  public DelegateProfileParams checkForProfile(
      String accountId, String delegateId, String profileId, long lastUpdatedAt) {
    logger.info("Checking delegate profile for account {}, delegate [{}]. Previous profile [{}] updated at {}",
        accountId, delegateId, profileId, lastUpdatedAt);
    Delegate delegate = get(accountId, delegateId, true);

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

    FileMetadata fileMetadata = new FileMetadata();
    fileMetadata.setFileName(new File(fileDetail.getFileName()).getName());
    String fileId = fileService.saveFile(fileMetadata,
        new BoundedInputStream(uploadedInputStream, mainConfiguration.getFileUploadLimits().getProfileResultLimit()),
        fileBucket);

    String previousProfileResult = delegate.getProfileResult();

    wingsPersistence.update(
        wingsPersistence.createQuery(Delegate.class).filter("accountId", accountId).filter(ID_KEY, delegateId),
        wingsPersistence.createUpdateOperations(Delegate.class)
            .set("profileResult", fileId)
            .set("profileError", error)
            .set("profileExecutedAt", clock.millis()));

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
                                                        .filter("accountId", accountId)
                                                        .filter(ID_KEY, heartbeat.getDelegateConnectionId()),
        wingsPersistence.createUpdateOperations(DelegateConnection.class)
            .set("lastHeartbeat", System.currentTimeMillis())
            .set("validUntil", Date.from(OffsetDateTime.now().plusMinutes(defaultExpiryTimeInMinutes).toInstant())));

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
      wingsPersistence.saveAndGet(DelegateConnection.class, connection);
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
        syncTaskWaitMap.get(delegateTask.getUuid()).wait(task.getTimeout());
      }
      completedTask = wingsPersistence.get(DelegateTask.class, task.getUuid());
    } catch (Exception e) {
      logger.error("Exception", e);
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, e).addParam("args", "Error while waiting for completion");
    } finally {
      syncTaskWaitMap.remove(delegateTask.getUuid());
      wingsPersistence.delete(wingsPersistence.createQuery(DelegateTask.class)
                                  .filter("accountId", delegateTask.getAccountId())
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
    task.setAsync(async);
    task.setVersion(getVersion());
    task.setBroadcastCount(1);
    task.setLastBroadcastAt(clock.millis());
    task.setPreAssignedDelegateId(assignDelegateService.pickFirstAttemptDelegate(task));
    DelegateTask delegateTask = wingsPersistence.saveAndGet(DelegateTask.class, task);
    logger.info("{} task: uuid: {}, accountId: {}, type: {}", async ? "Queueing async" : "Executing sync",
        delegateTask.getUuid(), delegateTask.getAccountId(), delegateTask.getTaskType());

    broadcasterFactory.lookup("/stream/delegate/" + delegateTask.getAccountId(), true).broadcast(delegateTask);
    return delegateTask;
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
                                       .filter("accountId", task.getAccountId())
                                       .field("lastHeartBeat")
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
          activeDelegates.size(), task.getUuid(), task.getTaskType(), task.getAccountId());
      alertService.openAlert(task.getAccountId(), task.getAppId(), NoEligibleDelegates,
          aNoEligibleDelegatesAlert()
              .withAppId(task.getAppId())
              .withEnvId(task.getEnvId())
              .withInfraMappingId(task.getInfrastructureMappingId())
              .withTaskGroup(TaskType.valueOf(task.getTaskType()).getTaskGroup())
              .withTaskType(TaskType.valueOf(task.getTaskType()))
              .build());
    }

    logger.info(
        "{} delegates {} eligible to execute task {}", eligibleDelegates.size(), eligibleDelegates, task.getTaskType());
    return eligibleDelegates;
  }

  @Override
  public DelegateTask acquireDelegateTask(String accountId, String delegateId, String taskId) {
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
      return delegateTask;
    } else {
      logger.info("Delegate {} is blacklisted for task {}", delegateId, taskId);
      return null;
    }
  }

  @Override
  public DelegateTask reportConnectionResults(
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
                                          .filter("accountId", delegateTask.getAccountId())
                                          .filter("status", QUEUED)
                                          .field("delegateId")
                                          .doesNotExist()
                                          .filter(ID_KEY, delegateTask.getUuid());
    wingsPersistence.update(updateQuery, updateOperations);

    if (results.stream().anyMatch(DelegateConnectionResult::isValidated)) {
      return assignTask(delegateId, taskId, delegateTask);
    }
    return null;
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
        List<String> criteria = TaskType.valueOf(delegateTask.getTaskType()).getCriteria(delegateTask, injector);
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
    UpdateOperations<DelegateTask> updateOperations =
        wingsPersistence.createUpdateOperations(DelegateTask.class).addToSet("validatingDelegateIds", delegateId);
    Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                          .filter("accountId", delegateTask.getAccountId())
                                          .filter("status", QUEUED)
                                          .field("delegateId")
                                          .doesNotExist()
                                          .filter(ID_KEY, delegateTask.getUuid());
    wingsPersistence.update(updateQuery, updateOperations);

    wingsPersistence.update(updateQuery.field("validationStartedAt").doesNotExist(),
        wingsPersistence.createUpdateOperations(DelegateTask.class).set("validationStartedAt", clock.millis()));
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
                                                          .unset("validatingDelegateIds")
                                                          .unset("validationCompleteDelegateIds");
    Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                          .filter("accountId", delegateTask.getAccountId())
                                          .filter("status", QUEUED)
                                          .field("delegateId")
                                          .doesNotExist()
                                          .filter(ID_KEY, delegateTask.getUuid());
    wingsPersistence.update(updateQuery, updateOperations);
  }

  private DelegateTask getUnassignedDelegateTask(String accountId, String taskId, String delegateId) {
    DelegateTask delegateTask =
        wingsPersistence.createQuery(DelegateTask.class).filter("accountId", accountId).filter(ID_KEY, taskId).get();

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

  private DelegateTask assignTask(String delegateId, String taskId, DelegateTask delegateTask) {
    // Clear pending validations. No longer need to track since we're assigning.
    clearFromValidationCache(delegateTask);

    logger.info("Assigning {} task {} to delegate {} {}", delegateTask.getTaskType(), taskId, delegateId,
        delegateTask.isAsync() ? "(async)" : "(sync)");
    Query<DelegateTask> query = wingsPersistence.createQuery(DelegateTask.class)
                                    .filter("accountId", delegateTask.getAccountId())
                                    .filter("status", QUEUED)
                                    .field("delegateId")
                                    .doesNotExist()
                                    .filter(ID_KEY, taskId);
    UpdateOperations<DelegateTask> updateOperations = wingsPersistence.createUpdateOperations(DelegateTask.class)
                                                          .set("delegateId", delegateId)
                                                          .set("status", STARTED);
    DelegateTask task =
        wingsPersistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL).findAndModify(query, updateOperations);
    // If the task wasn't updated because delegateId already exists then query for the task with the delegateId in case
    // client is retrying the request
    if (task == null) {
      task = wingsPersistence.createQuery(DelegateTask.class)
                 .filter("accountId", delegateTask.getAccountId())
                 .filter("status", STARTED)
                 .filter("delegateId", delegateId)
                 .filter(ID_KEY, taskId)
                 .get();
      if (task != null) {
        logger.info("Returning previously assigned task {} to delegate {}", taskId, delegateId);
      } else {
        logger.info("Task {} no longer available for delegate {}", taskId, delegateId);
      }
    } else {
      logger.info("Task {} assigned to delegate {}", taskId, delegateId);
    }
    return task;
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
                                        .filter("accountId", response.getAccountId())
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
        alreadyTriedDelegates.add(delegateId);

        List<String> remainingConnectedDelegates = assignDelegateService.connectedWhitelistedDelegates(delegateTask)
                                                       .stream()
                                                       .filter(item -> !alreadyTriedDelegates.contains(item))
                                                       .collect(toList());

        if (!remainingConnectedDelegates.isEmpty()) {
          logger.info("Requeueing task {}.", taskId);

          wingsPersistence.update(taskQuery,
              wingsPersistence.createUpdateOperations(DelegateTask.class)
                  .unset("delegateId")
                  .unset("validationStartedAt")
                  .unset("lastBroadcastAt")
                  .unset("validatingDelegateIds")
                  .unset("validationCompleteDelegateIds")
                  .set("broadcastCount", 1)
                  .set("status", QUEUED)
                  .addToSet("alreadyTriedDelegates", delegateId));
          return;
        } else {
          logger.info("Task {} has been tried on all the connected delegates. Proceeding with error.", taskId);
        }
      }

      if (delegateTask.isAsync()) {
        String waitId = delegateTask.getWaitId();
        if (waitId != null) {
          applyDelegateInfoToDelegateTaskResponse(delegateId, response);
          waitNotifyEngine.notify(waitId, response.getResponse());
        } else {
          logger.error("Async task {} has no wait ID", taskId);
        }
        wingsPersistence.delete(taskQuery);
      } else {
        wingsPersistence.update(taskQuery,
            wingsPersistence.createUpdateOperations(DelegateTask.class)
                .set("serializedNotifyResponseData", KryoUtils.asBytes(response.getResponse()))
                .set("status", FINISHED));
      }
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
               .filter("delegateId", delegateId)
               .filter("accountId", taskAbortEvent.getAccountId())
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
    wingsPersistence.update(
        delegateTaskQuery, wingsPersistence.createUpdateOperations(DelegateTask.class).set("status", error));

    broadcasterFactory.lookup("/stream/delegate/" + accountId, true)
        .broadcast(aDelegateTaskAbortEvent().withAccountId(accountId).withDelegateTaskId(delegateTaskId).build());
  }

  private Query<DelegateTask> getRunningTaskQuery(String accountId, String delegateTaskId) {
    Query<DelegateTask> delegateTaskQuery = wingsPersistence.createQuery(DelegateTask.class)
                                                .filter(ID_KEY, delegateTaskId)
                                                .filter("accountId", accountId)
                                                .filter("async", true);
    delegateTaskQuery.or(
        delegateTaskQuery.criteria("status").equal(QUEUED), delegateTaskQuery.criteria("status").equal(STARTED));
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
    Query<DelegateTask> delegateTaskQuery = wingsPersistence.createQuery(DelegateTask.class)
                                                .filter("accountId", accountId)
                                                .filter("status", QUEUED)
                                                .filter("async", !sync)
                                                .field("delegateId")
                                                .doesNotExist();

    if (featureFlagService.isEnabled(DELEGATE_TASK_VERSIONING, accountId)) {
      delegateTaskQuery.filter("version", versionInfoManager.getVersionInfo().getVersion());
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
    Query<DelegateTask> abortedQuery = wingsPersistence.createQuery(DelegateTask.class)
                                           .filter("status", ABORTED)
                                           .filter("async", true)
                                           .filter("accountId", accountId)
                                           .filter("delegateId", delegateId);

    // Send abort event only once by clearing delegateId
    wingsPersistence.update(
        abortedQuery, wingsPersistence.createUpdateOperations(DelegateTask.class).unset("delegateId"));

    return abortedQuery.project("accountId", true)
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

  @Override
  public void sendAlertNotificationsForDownDelegates(String accountId, List<Delegate> delegatesDown) {
    if (CollectionUtils.isNotEmpty(delegatesDown)) {
      List<AlertData> alertDatas = delegatesDown.stream()
                                       .map(delegate
                                           -> DelegatesDownAlert.builder()
                                                  .accountId(accountId)
                                                  .hostName(delegate.getHostName())
                                                  .ip(delegate.getIp())
                                                  .build())
                                       .collect(toList());

      // Find out new Alerts to be created
      List<AlertData> alertsToBeCreated = new ArrayList<>();
      for (AlertData alertData : alertDatas) {
        if (!alertService.findExistingAlert(accountId, GLOBAL_APP_ID, AlertType.DelegatesDown, alertData).isPresent()) {
          alertsToBeCreated.add(alertData);
        }
      }

      if (CollectionUtils.isNotEmpty(alertsToBeCreated)) {
        // create dashboard alerts
        alertService.openAlerts(accountId, GLOBAL_APP_ID, AlertType.DelegatesDown, alertsToBeCreated);
        sendDelegateDownNotification(accountId, alertsToBeCreated);
      }
    }
  }

  @Override
  public void sendAlertNotificationsForNoActiveDelegates(String accountId) {
    List<NotificationGroup> notificationGroups = notificationSetupService.listDefaultNotificationGroup(accountId);
    NotificationRule notificationRule = aNotificationRule().withNotificationGroups(notificationGroups).build();

    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(GLOBAL_APP_ID)
            .withAccountId(accountId)
            .withNotificationTemplateId(ALL_DELEGATE_DOWN_NOTIFICATION.name())
            .withNotificationTemplateVariables(ImmutableMap.of("ACCOUNT_ID", accountId))
            .build(),
        singletonList(notificationRule));
  }

  private void sendDelegateDownNotification(String accountId, List<AlertData> alertsToBeCreated) {
    // send slack/email notification
    String hostNamesForDownDelegates = "\n"
        + alertsToBeCreated.stream()
              .map(alertData -> ((DelegatesDownAlert) alertData).getHostName())
              .collect(joining("\n"));

    StringBuilder hostNamesForDownDelegatesHtml = new StringBuilder().append("<br />");
    alertsToBeCreated.forEach(alertData
        -> hostNamesForDownDelegatesHtml.append(((DelegatesDownAlert) alertData).getHostName()).append("<br />"));

    List<NotificationGroup> notificationGroups = notificationSetupService.listDefaultNotificationGroup(accountId);
    NotificationRule notificationRule = aNotificationRule().withNotificationGroups(notificationGroups).build();

    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(GLOBAL_APP_ID)
            .withAccountId(accountId)
            .withNotificationTemplateId(DELEGATE_STATE_NOTIFICATION.name())
            .withNotificationTemplateVariables(ImmutableMap.of("HOST_NAMES", hostNamesForDownDelegates,
                "HOST_NAMES_HTML", hostNamesForDownDelegatesHtml.toString(), "ENTITY_AFFECTED",
                English.plural("Delegate", alertsToBeCreated.size()), "DESCRIPTION_FIELD",
                English.plural("hostname", alertsToBeCreated.size()), "COUNT",
                Integer.toString(alertsToBeCreated.size())))
            .build(),
        singletonList(notificationRule));
  }

  private String getVersion() {
    return versionInfoManager.getVersionInfo().getVersion();
  }

  private void applyDelegateInfoToDelegateTaskResponse(String delegateId, DelegateTaskResponse response) {
    if (response != null && response.getResponse() instanceof DelegateTaskNotifyResponseData) {
      try {
        DelegateTaskNotifyResponseData delegateTaskNotifyResponseData =
            (DelegateTaskNotifyResponseData) response.getResponse();
        Optional<Delegate> delegate = delegateCache.get(delegateId);
        delegateTaskNotifyResponseData.setDelegateMetaInfo(
            DelegateMetaInfo.builder()
                .id(delegateId)
                .hostName(delegate.isPresent() ? delegate.get().getHostName() : delegateId)
                .build());
      } catch (ExecutionException e) {
        logger.error("Execution exception", e);
      } catch (UncheckedExecutionException e) {
        logger.error("Delegate not found exception", e);
      }
    }
  }
}
