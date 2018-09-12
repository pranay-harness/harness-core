package software.wings.service.impl;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.UNAVAILABLE_DELEGATES;
import static io.harness.exception.WingsException.NOBODY;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.threading.Morpheus.sleep;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMillis;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Delegate.HOST_NAME_KEY;
import static software.wings.beans.DelegateConnection.defaultExpiryTimeInMinutes;
import static software.wings.beans.DelegateTask.Status.ABORTED;
import static software.wings.beans.DelegateTask.Status.ERROR;
import static software.wings.beans.DelegateTask.Status.FINISHED;
import static software.wings.beans.DelegateTask.Status.QUEUED;
import static software.wings.beans.DelegateTask.Status.STARTED;
import static software.wings.beans.DelegateTaskAbortEvent.Builder.aDelegateTaskAbortEvent;
import static software.wings.beans.DelegateTaskEvent.DelegateTaskEventBuilder.aDelegateTaskEvent;
import static software.wings.beans.DelegateTaskResponse.Builder.aDelegateTaskResponse;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.FeatureName.DELEGATE_TASK_VERSIONING;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.alert.AlertType.NoEligibleDelegates;
import static software.wings.beans.alert.NoEligibleDelegatesAlert.NoEligibleDelegatesAlertBuilder.aNoEligibleDelegatesAlert;
import static software.wings.common.Constants.DELEGATE_DIR;
import static software.wings.common.Constants.DOCKER_DELEGATE;
import static software.wings.common.Constants.KUBERNETES_DELEGATE;
import static software.wings.common.Constants.MAX_DELEGATE_LAST_HEARTBEAT;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.ALL_DELEGATE_DOWN_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.DELEGATE_STATE_NOTIFICATION;
import static software.wings.delegatetasks.RemoteMethodReturnValueData.Builder.aRemoteMethodReturnValueData;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.utils.KubernetesConvention.getAccountIdentifier;

import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.github.zafarkhaja.semver.Version;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.version.VersionInfoManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atteo.evo.inflector.English;
import org.mongodb.morphia.mapping.Mapper;
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
import software.wings.beans.DelegateInitialization;
import software.wings.beans.DelegateProfile;
import software.wings.beans.DelegateScripts;
import software.wings.beans.DelegateStatus;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.Event.Type;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DelegatesDownAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.impl.infra.InfraDownloadService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.sm.DelegateMetaInfo;
import software.wings.utils.KryoUtils;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.NotFoundException;

@Singleton
@ValidateOnExecution
public class DelegateServiceImpl implements DelegateService {
  private static final Logger logger = LoggerFactory.getLogger(DelegateServiceImpl.class);

  private static final String ACCOUNT_ID = "accountId";
  private static final Configuration cfg = new Configuration(VERSION_2_3_23);
  private static final int MAX_DELEGATE_META_INFO_ENTRIES = 10000;
  private static final int DELEGATE_METADATA_HTTP_CALL_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);

  static {
    cfg.setTemplateLoader(new ClassTemplateLoader(DelegateServiceImpl.class, "/delegatetemplates"));
  }

  public static final long VALIDATION_TIMEOUT = TimeUnit.SECONDS.toMillis(12);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private AccountService accountService;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private EventEmitter eventEmitter;
  @Inject private BroadcasterFactory broadcasterFactory;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private AlertService alertService;
  @Inject private NotificationService notificationService;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private Clock clock;
  @Inject private VersionInfoManager versionInfoManager;
  @Inject private Injector injector;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private InfraDownloadService infraDownloadService;
  @Inject private DelegateProfileService delegateProfileService;

  private LoadingCache<String, String> delegateVersionCache =
      CacheBuilder.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(1, TimeUnit.MINUTES)
          .build(new CacheLoader<String, String>() {
            public String load(String accountId) {
              return fetchAccountDelegateMetadataFromStorage(accountId);
            }
          });

  private LoadingCache<String, DelegateMetaInfo> delegateMetaInfoCache =
      CacheBuilder.newBuilder()
          .maximumSize(MAX_DELEGATE_META_INFO_ENTRIES)
          .build(new CacheLoader<String, DelegateMetaInfo>() {
            public DelegateMetaInfo load(String delegateId) throws NotFoundException {
              Delegate delegate = wingsPersistence.createQuery(Delegate.class)
                                      .filter(ID_KEY, delegateId)
                                      .project(ID_KEY, true)
                                      .project(Delegate.VERSION_KEY, true)
                                      .project(HOST_NAME_KEY, true)
                                      .get();

              if (delegate != null) {
                return DelegateMetaInfo.builder()
                    .id(delegate.getUuid())
                    .hostName(delegate.getHostName())
                    .version(delegate.getVersion())
                    .build();
              } else {
                throw new NotFoundException("Delegate with id " + delegateId + " not found");
              }
            }
          });

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
  public Delegate get(String accountId, String delegateId) {
    return wingsPersistence.createQuery(Delegate.class)
        .filter("accountId", accountId)
        .filter(Mapper.ID_KEY, delegateId)
        .get();
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

    return get(accountId, delegateId);
  }

  @Override
  public Delegate updateHeartbeat(String accountId, String delegateId) {
    wingsPersistence.update(
        wingsPersistence.createQuery(Delegate.class).filter("accountId", accountId).filter(ID_KEY, delegateId),
        wingsPersistence.createUpdateOperations(Delegate.class)
            .set("lastHeartBeat", System.currentTimeMillis())
            .set("connected", true));
    return get(accountId, delegateId);
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
    return get(delegate.getAccountId(), delegate.getUuid());
  }

  @Override
  public DelegateScripts getDelegateScripts(String accountId, String version, String managerHost)
      throws IOException, TemplateException {
    ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(accountId, version, managerHost);

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
      String accountId, String version, String managerHost) {
    return getJarAndScriptRunTimeParamMap(accountId, version, managerHost, null, null);
  }

  private ImmutableMap<String, String> getJarAndScriptRunTimeParamMap(
      String accountId, String version, String managerHost, String delegateName, String delegateProfile) {
    String latestVersion = null;
    String jarRelativePath;
    String delegateJarDownloadUrl = null;
    String delegateStorageUrl = null;
    String delegateCheckLocation = null;
    boolean jarFileExists = false;
    boolean versionChanged = false;

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
      ImmutableMap.Builder<String, String> params = ImmutableMap.<String, String>builder()
                                                        .put("accountId", accountId)
                                                        .put("accountSecret", account.getAccountKey())
                                                        .put("upgradeVersion", latestVersion)
                                                        .put("managerHostAndPort", managerHost)
                                                        .put("watcherStorageUrl", watcherStorageUrl)
                                                        .put("watcherCheckLocation", watcherCheckLocation)
                                                        .put("delegateStorageUrl", delegateStorageUrl)
                                                        .put("delegateCheckLocation", delegateCheckLocation)
                                                        .put("deployMode", mainConfiguration.getDeployMode().name())
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
  public File downloadScripts(String managerHost, String accountId) throws IOException, TemplateException {
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

      ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(accountId, version, managerHost);

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
  public File downloadDocker(String managerHost, String accountId) throws IOException, TemplateException {
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

      ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(accountId, version, managerHost);

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
  public File downloadKubernetes(String managerHost, String accountId, String delegateName, String delegateProfile)
      throws IOException, TemplateException {
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

      ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(
          accountId, version, managerHost, delegateName, delegateProfile == null ? "" : delegateProfile);

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
    logger.info("Adding delegate: {}", delegate.getUuid());
    delegate.setAppId(GLOBAL_APP_ID);
    Delegate savedDelegate = wingsPersistence.saveAndGet(Delegate.class, delegate);
    eventEmitter.send(Channel.DELEGATES,
        anEvent().withOrgId(delegate.getAccountId()).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
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
    }

    wingsPersistence.delete(
        wingsPersistence.createQuery(Delegate.class).filter("accountId", accountId).filter(ID_KEY, delegateId));
  }

  @Override
  public Delegate register(Delegate delegate) {
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
    return registeredDelegate;
  }

  @Override
  public DelegateInitialization checkForProfile(
      String accountId, String delegateId, String profileId, long lastUpdatedAt) {
    logger.info("Checking delegate profile for account {}, delegate [{}]. Previous profile [{}] updated at {}",
        accountId, delegateId, profileId, lastUpdatedAt);
    Delegate delegate = get(accountId, delegateId);
    if (isNotBlank(delegate.getDelegateProfileId())) {
      DelegateProfile profile = delegateProfileService.get(accountId, delegate.getDelegateProfileId());
      if (profile != null && (!profile.getUuid().equals(profileId) || profile.getLastUpdatedAt() > lastUpdatedAt)) {
        return DelegateInitialization.builder()
            .profileId(profile.getUuid())
            .name(profile.getName())
            .profileLastUpdatedAt(profile.getLastUpdatedAt())
            .scriptContent(profile.getStartupScript())
            .build();
      }
    }
    return null;
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
    task.setAsync(true);
    task.setVersion(getVersion());
    DelegateTask delegateTask = wingsPersistence.saveAndGet(DelegateTask.class, task);
    logger.info("Queueing async task uuid: {}, accountId: {}, type: {}", delegateTask.getUuid(),
        delegateTask.getAccountId(), delegateTask.getTaskType());

    broadcasterFactory.lookup("/stream/delegate/" + delegateTask.getAccountId(), true).broadcast(delegateTask);

    wingsPersistence.update(delegateTask,
        wingsPersistence.createUpdateOperations(DelegateTask.class)
            .set("lastBroadcastAt", clock.millis())
            .set("broadcastCount", 1));

    return delegateTask.getUuid();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends NotifyResponseData> T executeTask(DelegateTask task) {
    List<String> eligibleDelegateIds = ensureDelegateAvailableToExecuteTask(task);
    if (isEmpty(eligibleDelegateIds)) {
      throw new WingsException(UNAVAILABLE_DELEGATES, USER_ADMIN);
    }
    task.setAsync(false);
    task.setVersion(getVersion());
    DelegateTask delegateTask = wingsPersistence.saveAndGet(DelegateTask.class, task);
    logger.info("Executing sync task: uuid: {}, accountId: {}, type: {}", delegateTask.getUuid(),
        delegateTask.getAccountId(), delegateTask.getTaskType());

    broadcasterFactory.lookup("/stream/delegate/" + delegateTask.getAccountId(), true).broadcast(delegateTask);

    wingsPersistence.update(delegateTask,
        wingsPersistence.createUpdateOperations(DelegateTask.class)
            .set("lastBroadcastAt", clock.millis())
            .set("broadcastCount", 1));

    // Wait for task to complete
    AtomicReference<DelegateTask> delegateTaskRef = new AtomicReference<>(delegateTask);
    try {
      timeLimiter.callWithTimeout(() -> {
        while (delegateTaskRef.get() == null || !isTaskComplete(delegateTaskRef.get().getStatus())) {
          sleep(ofMillis(500));
          delegateTaskRef.set(wingsPersistence.get(DelegateTask.class, task.getUuid()));
          if (delegateTaskRef.get() != null) {
            logger.info(
                "Delegate task [{}] - status [{}]", delegateTaskRef.get().getUuid(), delegateTaskRef.get().getStatus());
          }
        }
        return true;
      }, task.getTimeout(), TimeUnit.MILLISECONDS, true);
    } catch (UncheckedTimeoutException e) {
      logger.info("Timed out waiting for sync task {}", delegateTask.getUuid());
    } catch (Exception e) {
      logger.error("Exception", e);
    }

    if (delegateTaskRef.get() == null) {
      logger.info("Task {} was deleted while waiting for completion", delegateTask.getUuid());
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Task was deleted while waiting for completion");
    }

    wingsPersistence.delete(wingsPersistence.createQuery(DelegateTask.class)
                                .filter("accountId", delegateTask.getAccountId())
                                .filter(ID_KEY, delegateTask.getUuid()));

    NotifyResponseData responseData = delegateTaskRef.get().getNotifyResponse();
    if (responseData == null) {
      throw new WingsException(ErrorCode.REQUEST_TIMEOUT, WingsException.USER_ADMIN)
          .addParam("name", "Harness delegate");
    }

    logger.info("Returned response to calling function for delegate task [{}] ", delegateTask.getUuid());
    return (T) responseData;
  }

  private boolean isTaskComplete(DelegateTask.Status status) {
    return status != null && (status.equals(FINISHED) || status.equals(ABORTED) || status.equals(ERROR));
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
              .build());
    }

    logger.info(
        "{} delegates {} eligible to execute the task {}", eligibleDelegates.size(), eligibleDelegates, task.getUuid());
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
        NotifyResponseData response;
        if (delegateTask.isAsync()) {
          response = ErrorNotifyResponseData.builder().errorMessage(errorMessage).build();
        } else {
          InvalidRequestException exception = new InvalidRequestException(errorMessage, USER);
          response = aRemoteMethodReturnValueData().withException(exception).build();
        }
        processDelegateResponse(
            accountId, null, taskId, aDelegateTaskResponse().withAccountId(accountId).withResponse(response).build());
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
    DelegateTask task = wingsPersistence.getDatastore().findAndModify(query, updateOperations);
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
  public void clearCache(String delegateId) {
    assignDelegateService.clearConnectionResults(delegateId);
  }

  @Override
  public void processDelegateResponse(
      String accountId, String delegateId, String taskId, DelegateTaskResponse response) {
    if (response == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, NOBODY).addParam("args", "response cannot be null");
    }

    logger.info("Delegate [{}], response received for taskId [{}]", delegateId, taskId);

    Query<DelegateTask> taskQuery = wingsPersistence.createQuery(DelegateTask.class)
                                        .filter("accountId", response.getAccountId())
                                        .filter(ID_KEY, taskId);

    DelegateTask delegateTask = taskQuery.get();

    if (delegateTask != null) {
      if (!StringUtils.equals(delegateTask.getVersion(), getVersion())) {
        logger.warn("Version mismatch for task {} in account {}. [managerVersion {}, taskVersion {}]",
            delegateTask.getUuid(), delegateTask.getAccountId(), getVersion(), delegateTask.getVersion());
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
    return wingsPersistence.createQuery(Delegate.class)
               .filter("accountId", task.getAccountId())
               .filter(ID_KEY, delegateId)
               .filter("status", Status.ENABLED)
               .getKey()
        != null;
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
        delegateTaskNotifyResponseData.setDelegateMetaInfo(delegateMetaInfoCache.get(delegateId));
      } catch (ExecutionException e) {
        logger.error("Execution exception", e);
      } catch (UncheckedExecutionException e) {
        logger.error("Delegate not found exception", e);
      }
    }
  }
}
