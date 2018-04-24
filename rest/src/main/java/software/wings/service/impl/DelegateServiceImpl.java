package software.wings.service.impl;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Status.ABORTED;
import static software.wings.beans.DelegateTask.Status.ERROR;
import static software.wings.beans.DelegateTask.Status.FINISHED;
import static software.wings.beans.DelegateTask.Status.QUEUED;
import static software.wings.beans.DelegateTask.Status.STARTED;
import static software.wings.beans.DelegateTaskAbortEvent.Builder.aDelegateTaskAbortEvent;
import static software.wings.beans.DelegateTaskEvent.DelegateTaskEventBuilder.aDelegateTaskEvent;
import static software.wings.beans.ErrorCode.UNAVAILABLE_DELEGATES;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.alert.AlertType.NoEligibleDelegates;
import static software.wings.beans.alert.NoEligibleDelegatesAlert.NoEligibleDelegatesAlertBuilder.aNoEligibleDelegatesAlert;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.DELEGATE_STATE_NOTIFICATION;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.exception.WingsException.USER_ADMIN;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.zafarkhaja.semver.Version;
import com.mongodb.BasicDBObject;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.archivers.zip.AsiExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Request;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atteo.evo.inflector.English;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateScripts;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.ErrorCode;
import software.wings.beans.Event.Type;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DelegatesDownAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.common.Constants;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
@Singleton
@ValidateOnExecution
public class DelegateServiceImpl implements DelegateService {
  private static final Logger logger = LoggerFactory.getLogger(DelegateServiceImpl.class);

  private static final Configuration cfg = new Configuration(VERSION_2_3_23);

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

  @Override
  public PageResponse<Delegate> list(PageRequest<Delegate> pageRequest) {
    return wingsPersistence.query(Delegate.class, pageRequest);
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
    setUnset(updateOperations, "status", delegate.getStatus());
    setUnset(updateOperations, "lastHeartBeat", delegate.getLastHeartBeat());
    setUnset(updateOperations, "connected", delegate.isConnected());
    setUnset(updateOperations, "supportedTaskTypes", delegate.getSupportedTaskTypes());
    setUnset(updateOperations, "version", delegate.getVersion());
    setUnset(updateOperations, "description", delegate.getDescription());

    logger.info("Updating delegate : {}", delegate.getUuid());
    return updateDelegate(delegate, updateOperations);
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
  public DelegateScripts checkForUpgrade(String accountId, String delegateId, String version, String managerHost)
      throws IOException, TemplateException {
    logger.info("Checking delegate for upgrade: {}", delegateId);

    ImmutableMap<Object, Object> scriptParams = getJarAndScriptRunTimeParamMap(accountId, version, managerHost);

    DelegateScripts delegateScripts = new DelegateScripts();
    delegateScripts.setDelegateId(delegateId);
    delegateScripts.setVersion(version);
    delegateScripts.setDoUpgrade(false);
    if (isNotEmpty(scriptParams)) {
      logger.info("Upgrading delegate to version: {}", scriptParams.get("upgradeVersion"));
      delegateScripts.setDoUpgrade(true);
      delegateScripts.setVersion((String) scriptParams.get("upgradeVersion"));

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

  public String getLatestDelegateVersion() {
    try {
      String delegateMetadataUrl = mainConfiguration.getDelegateMetadataUrl().trim();
      String delegateMatadata = Request.Get(delegateMetadataUrl)
                                    .connectTimeout(10000)
                                    .socketTimeout(10000)
                                    .execute()
                                    .returnContent()
                                    .asString()
                                    .trim();
      return substringBefore(delegateMatadata, " ").trim();
    } catch (IOException e) {
      return null;
    }
  }

  private ImmutableMap<Object, Object> getJarAndScriptRunTimeParamMap(
      String accountId, String version, String managerHost) {
    String latestVersion = null;
    String jarRelativePath;
    String delegateJarDownloadUrl = null;
    boolean jarFileExists = false;
    String harnessApiUrl = "https://api.harness.io";

    if (isNotBlank(mainConfiguration.getApiUrl())) {
      harnessApiUrl = mainConfiguration.getApiUrl();
    }

    try {
      String delegateMetadataUrl = mainConfiguration.getDelegateMetadataUrl().trim();
      logger.info("Delegate metaData URL is " + delegateMetadataUrl);
      String delegateMatadata = Request.Get(delegateMetadataUrl)
                                    .connectTimeout(10000)
                                    .socketTimeout(10000)
                                    .execute()
                                    .returnContent()
                                    .asString()
                                    .trim();
      logger.info("Delegate meta data: [{}]", delegateMatadata);

      latestVersion = substringBefore(delegateMatadata, " ").trim();
      jarRelativePath = substringAfter(delegateMatadata, " ").trim();
      delegateJarDownloadUrl =
          delegateMetadataUrl.substring(0, delegateMetadataUrl.lastIndexOf('/')) + "/" + jarRelativePath;
      jarFileExists = Request.Head(delegateJarDownloadUrl)
                          .connectTimeout(10000)
                          .socketTimeout(10000)
                          .execute()
                          .handleResponse(response -> response.getStatusLine().getStatusCode() == 200);
    } catch (IOException e) {
      logger.warn("Unable to fetch delegate version information", e);
      logger.warn("CurrentVersion: [{}], LatestVersion=[{}], delegateJarDownloadUrl=[{}]", version, latestVersion,
          delegateJarDownloadUrl);
    }

    logger.info("Found delegate latest version: [{}] url: [{}]", latestVersion, delegateJarDownloadUrl);
    boolean doUpgrade = false;
    if (jarFileExists) {
      doUpgrade = !(Version.valueOf(version).equals(Version.valueOf(latestVersion)));
    }

    if (doUpgrade) {
      String watcherLatestVersion = "";
      String watcherJarRelativePath;
      String watcherJarDownloadUrl = "";
      String watcherMetadataUrl = "";

      try {
        watcherMetadataUrl = mainConfiguration.getWatcherMetadataUrl().trim();
        String watcherMetadata = Request.Get(watcherMetadataUrl)
                                     .connectTimeout(10000)
                                     .socketTimeout(10000)
                                     .execute()
                                     .returnContent()
                                     .asString()
                                     .trim();
        logger.info("Watcher meta data: [{}]", watcherMetadata);

        watcherLatestVersion = substringBefore(watcherMetadata, " ").trim();
        watcherJarRelativePath = substringAfter(watcherMetadata, " ").trim();
        watcherJarDownloadUrl =
            watcherMetadataUrl.substring(0, watcherMetadataUrl.lastIndexOf('/')) + "/" + watcherJarRelativePath;
      } catch (IOException e) {
        logger.warn("Unable to fetch watcher version information", e);
        logger.warn("LatestVersion=[{}], watcherJarDownloadUrl=[{}]", watcherLatestVersion, watcherJarDownloadUrl);
      }

      logger.info("Found watcher latest version: [{}] url: [{}]", watcherLatestVersion, watcherJarDownloadUrl);

      Account account = accountService.get(accountId);
      return ImmutableMap.builder()
          .put("accountId", accountId)
          .put("accountSecret", account.getAccountKey())
          .put("upgradeVersion", latestVersion)
          .put("currentVersion", version)
          .put("delegateJarUrl", delegateJarDownloadUrl)
          .put("managerHostAndPort", managerHost)
          .put("watcherJarUrl", watcherJarDownloadUrl)
          .put("watcherUpgradeVersion", watcherLatestVersion)
          .put("watcherCheckLocation", watcherMetadataUrl)
          .put("harnessApiUrl", harnessApiUrl)
          .put("deployMode", mainConfiguration.getDeployMode())
          .build();
    }
    return null;
  }

  @Override
  public File download(String managerHost, String accountId) throws IOException, TemplateException {
    File delegateFile = File.createTempFile(Constants.DELEGATE_DIR, ".zip");

    try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(delegateFile)) {
      out.putArchiveEntry(new ZipArchiveEntry(Constants.DELEGATE_DIR + "/"));
      out.closeArchiveEntry();

      Map scriptParams = getJarAndScriptRunTimeParamMap(accountId, "0.0.0", managerHost); // first version is 0.0.0

      File start = File.createTempFile("start", ".sh");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(start))) {
        cfg.getTemplate("start.sh.ftl").process(scriptParams, fileWriter);
      }
      start = new File(start.getAbsolutePath());
      ZipArchiveEntry startZipArchiveEntry = new ZipArchiveEntry(start, Constants.DELEGATE_DIR + "/start.sh");
      startZipArchiveEntry.setUnixMode(0755 << 16L);
      AsiExtraField permissions = new AsiExtraField();
      permissions.setMode(0755);
      startZipArchiveEntry.addExtraField(permissions);
      out.putArchiveEntry(startZipArchiveEntry);
      try (FileInputStream fis = new FileInputStream(start)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File delegate = File.createTempFile("delegate", ".sh");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(delegate))) {
        cfg.getTemplate("delegate.sh.ftl").process(scriptParams, fileWriter);
      }
      delegate = new File(delegate.getAbsolutePath());
      ZipArchiveEntry delegateZipArchiveEntry = new ZipArchiveEntry(delegate, Constants.DELEGATE_DIR + "/delegate.sh");
      delegateZipArchiveEntry.setUnixMode(0755 << 16L);
      permissions = new AsiExtraField();
      permissions.setMode(0755);
      delegateZipArchiveEntry.addExtraField(permissions);
      out.putArchiveEntry(delegateZipArchiveEntry);
      try (FileInputStream fis = new FileInputStream(delegate)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File stop = File.createTempFile("stop", ".sh");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(stop))) {
        cfg.getTemplate("stop.sh.ftl").process(scriptParams, fileWriter);
      }
      stop = new File(stop.getAbsolutePath());
      ZipArchiveEntry stopZipArchiveEntry = new ZipArchiveEntry(stop, Constants.DELEGATE_DIR + "/stop.sh");
      stopZipArchiveEntry.setUnixMode(0755 << 16L);
      permissions = new AsiExtraField();
      permissions.setMode(0755);
      stopZipArchiveEntry.addExtraField(permissions);
      out.putArchiveEntry(stopZipArchiveEntry);
      try (FileInputStream fis = new FileInputStream(stop)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File readme = File.createTempFile("README", ".txt");
      try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(readme))) {
        cfg.getTemplate("readme.txt.ftl").process(ImmutableMap.of("startScript", "start.sh"), fileWriter);
      }
      readme = new File(readme.getAbsolutePath());
      ZipArchiveEntry readmeZipArchiveEntry = new ZipArchiveEntry(readme, Constants.DELEGATE_DIR + "/README.txt");
      out.putArchiveEntry(readmeZipArchiveEntry);
      try (FileInputStream fis = new FileInputStream(readme)) {
        IOUtils.copy(fis, out);
      }

      File proxyConfig = File.createTempFile("proxy", ".config");
      try (BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(proxyConfig)))) {
        fileWriter.write("PROXY_HOST=");
        fileWriter.newLine();
        fileWriter.write("PROXY_PORT=");
        fileWriter.newLine();
        fileWriter.write("PROXY_SCHEME=");
        fileWriter.newLine();
        fileWriter.write("NO_PROXY=");
      }

      proxyConfig = new File(proxyConfig.getAbsolutePath());
      ZipArchiveEntry proxyZipArchiveEntry = new ZipArchiveEntry(proxyConfig, Constants.DELEGATE_DIR + "/proxy.config");
      proxyZipArchiveEntry.setUnixMode(0644 << 16L);
      permissions = new AsiExtraField();
      permissions.setMode(0644);
      proxyZipArchiveEntry.addExtraField(permissions);
      out.putArchiveEntry(proxyZipArchiveEntry);
      try (FileInputStream fis = new FileInputStream(proxyConfig)) {
        IOUtils.copy(fis, out);
      }

      out.closeArchiveEntry();
    }
    return delegateFile;
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
  @SuppressWarnings("unchecked")
  public void delete(String accountId, String delegateId) {
    logger.info("Deleting delegate: {}", delegateId);
    Delegate existingDelegate = wingsPersistence.get(Delegate.class,
        aPageRequest()
            .addFilter("accountId", EQ, accountId)
            .addFilter(ID_KEY, EQ, delegateId)
            .addFieldsExcluded("supportedTaskTypes")
            .build());

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
    Delegate existingDelegate = wingsPersistence.createQuery(Delegate.class)
                                    .filter("accountId", delegate.getAccountId())
                                    .filter("ip", delegate.getIp())
                                    .filter("hostName", delegate.getHostName())
                                    .project("status", true)
                                    .get();
    Delegate registeredDelegate;
    if (existingDelegate == null) {
      logger.info("No existing delegate, adding for account {}: Hostname: {} IP: {}", delegate.getAccountId(),
          delegate.getHostName(), delegate.getIp());
      registeredDelegate = add(delegate);
    } else {
      logger.info("Delegate exists, updating: {}", delegate.getUuid());
      delegate.setUuid(existingDelegate.getUuid());
      delegate.setStatus(existingDelegate.getStatus());
      registeredDelegate = update(delegate);
      broadcasterFactory.lookup("/stream/delegate/" + delegate.getAccountId(), true)
          .broadcast("[X]" + delegate.getUuid());
    }
    alertService.activeDelegateUpdated(registeredDelegate.getAccountId(), registeredDelegate.getUuid());
    return registeredDelegate;
  }

  @Override
  @SuppressWarnings("unchecked")
  public PageResponse<DelegateTask> getDelegateTasks(String accountId, String delegateId) {
    return wingsPersistence.query(DelegateTask.class,
        aPageRequest().addFilter("accountId", EQ, accountId).addFilter("delegateId", EQ, delegateId).build());
  }

  @Override
  public String queueTask(DelegateTask task) {
    task.setAsync(true);
    DelegateTask delegateTask = wingsPersistence.saveAndGet(DelegateTask.class, task);
    logger.info("Queueing async task uuid: {}, accountId: {}, type: {}", delegateTask.getUuid(),
        delegateTask.getAccountId(), delegateTask.getTaskType());
    broadcasterFactory.lookup("/stream/delegate/" + delegateTask.getAccountId(), true).broadcast(delegateTask);
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
    DelegateTask delegateTask = wingsPersistence.saveAndGet(DelegateTask.class, task);

    broadcasterFactory.lookup("/stream/delegate/" + delegateTask.getAccountId(), true).broadcast(delegateTask);
    logger.info("Executing sync task: uuid: {}, accountId: {}, type: {}", delegateTask.getUuid(),
        delegateTask.getAccountId(), delegateTask.getTaskType());

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
      throw new WingsException(ErrorCode.REQUEST_TIMEOUT).addParam("name", Constants.DELEGATE_NAME);
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
    List<String> activeDelegates = wingsPersistence.createQuery(Delegate.class)
                                       .filter("accountId", task.getAccountId())
                                       .filter("connected", true)
                                       .filter("status", Status.ENABLED)
                                       .field("supportedTaskTypes")
                                       .contains(task.getTaskType().name())
                                       .field("lastHeartBeat")
                                       .greaterThan(clock.millis() - Constants.MAX_DELEGATE_LAST_HEARTBEAT)
                                       .asKeyList()
                                       .stream()
                                       .map(key -> key.getId().toString())
                                       .collect(toList());

    logger.info("{} delegates {} are active", activeDelegates.size(), activeDelegates);

    List<String> eligibleDelegates = activeDelegates.stream()
                                         .filter(delegateId -> assignDelegateService.canAssign(delegateId, task))
                                         .collect(toList());

    if (activeDelegates.isEmpty()) {
      logger.warn("No delegates are active for the account: {}", task.getAccountId());
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
              .withTaskGroup(task.getTaskType().getTaskGroup())
              .build());
    }

    logger.info("{} delegates {} eligible to execute the task", eligibleDelegates.size(), eligibleDelegates);
    return eligibleDelegates;
  }

  @Override
  public DelegateTask acquireDelegateTask(String accountId, String delegateId, String taskId) {
    logger.info("Acquiring delegate task {} for delegate {}", taskId, delegateId);
    DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId);
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
    } else {
      setValidationStarted(delegateId, delegateTask);
      return delegateTask;
    }
  }

  @Override
  public DelegateTask reportConnectionResults(
      String accountId, String delegateId, String taskId, List<DelegateConnectionResult> results) {
    assignDelegateService.saveConnectionResults(results);
    DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId);
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
  public DelegateTask shouldProceedAnyway(String accountId, String delegateId, String taskId) {
    DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId);
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
        return assignTask(delegateId, taskId, delegateTask);
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

  private DelegateTask getUnassignedDelegateTask(String accountId, String taskId) {
    DelegateTask delegateTask = wingsPersistence.createQuery(DelegateTask.class)
                                    .filter("accountId", accountId)
                                    .filter("status", QUEUED)
                                    .field("delegateId")
                                    .doesNotExist()
                                    .filter(ID_KEY, taskId)
                                    .get();

    if (delegateTask == null) {
      logger.info("Delegate task {} is already assigned", taskId);
      return null;
    }

    logger.info("Found unassigned delegate task: {}", delegateTask.getUuid());
    return delegateTask;
  }

  private DelegateTask assignTask(String delegateId, String taskId, DelegateTask delegateTask) {
    // Clear pending validations. No longer need to track since we're assigning.
    clearFromValidationCache(delegateTask);

    logger.info(
        "Assigning task {} to delegate {} {}", taskId, delegateId, delegateTask.isAsync() ? "(async)" : "(sync)");
    Query<DelegateTask> query = wingsPersistence.createQuery(DelegateTask.class)
                                    .filter("accountId", delegateTask.getAccountId())
                                    .filter("status", QUEUED)
                                    .field("delegateId")
                                    .doesNotExist()
                                    .filter(ID_KEY, taskId);
    UpdateOperations<DelegateTask> updateOperations =
        wingsPersistence.createUpdateOperations(DelegateTask.class).set("delegateId", delegateId);
    return wingsPersistence.getDatastore().findAndModify(query, updateOperations);
  }

  @Override
  public DelegateTask startDelegateTask(String accountId, String delegateId, String taskId) {
    logger.info("Starting task {} with delegate {}", taskId, delegateId);
    Query<DelegateTask> query = wingsPersistence.createQuery(DelegateTask.class)
                                    .filter("accountId", accountId)
                                    .filter("status", QUEUED)
                                    .filter("delegateId", delegateId)
                                    .filter(ID_KEY, taskId);
    UpdateOperations<DelegateTask> updateOperations =
        wingsPersistence.createUpdateOperations(DelegateTask.class).set("status", STARTED);
    return wingsPersistence.getDatastore().findAndModify(query, updateOperations);
  }

  @Override
  public void clearCache(String delegateId) {
    assignDelegateService.clearConnectionResults(delegateId);
  }

  @Override
  public void processDelegateResponse(DelegateTaskResponse response) {
    DelegateTask task = response.getTask();
    logger.info(
        "Delegate [{}], response received for task [{}, {}]", task.getDelegateId(), task.getUuid(), task.getTaskType());

    DelegateTask delegateTask = wingsPersistence.createQuery(DelegateTask.class)
                                    .filter("accountId", response.getAccountId())
                                    .filter(ID_KEY, task.getUuid())
                                    .get();

    if (delegateTask != null) {
      if (delegateTask.isAsync()) {
        String waitId = delegateTask.getWaitId();
        if (waitId != null) {
          waitNotifyEngine.notify(waitId, response.getResponse());
        } else {
          logger.error("Async task {} with type {} has no wait ID", task.getUuid(), task.getTaskType().name());
        }
        wingsPersistence.delete(wingsPersistence.createQuery(DelegateTask.class)
                                    .filter("accountId", response.getAccountId())
                                    .filter(ID_KEY, delegateTask.getUuid()));
      } else {
        delegateTask.setNotifyResponse(response.getResponse());
        delegateTask.setStatus(FINISHED);
        wingsPersistence.save(delegateTask);
      }
    } else {
      logger.warn("No delegate task found: {}", task.getUuid());
    }
  }

  @Override
  public boolean filter(String delegateId, DelegateTask task) {
    boolean qualifies = false;
    Delegate delegate = wingsPersistence.get(Delegate.class,
        aPageRequest()
            .addFilter("accountId", IN, task.getAccountId(), GLOBAL_ACCOUNT_ID)
            .addFilter(ID_KEY, EQ, delegateId)
            .addFilter("status", EQ, Status.ENABLED)
            .build());

    if (delegate != null && delegate.getSupportedTaskTypes().contains(task.getTaskType())) {
      qualifies = true;
    }

    return qualifies;
  }

  @Override
  public boolean filter(String delegateId, DelegateTaskAbortEvent taskAbortEvent) {
    return wingsPersistence.get(DelegateTask.class,
               aPageRequest()
                   .addFilter(ID_KEY, EQ, taskAbortEvent.getDelegateTaskId())
                   .addFilter("delegateId", EQ, delegateId)
                   .build())
        != null;
  }

  @Override
  public void abortTask(String accountId, String delegateTaskId) {
    logger.info("Aborting delegate task {}", delegateTaskId);
    Query<DelegateTask> delegateTaskQuery = wingsPersistence.createQuery(DelegateTask.class)
                                                .filter(ID_KEY, delegateTaskId)
                                                .filter("accountId", accountId)
                                                .filter("async", true);
    delegateTaskQuery.or(
        delegateTaskQuery.criteria("status").equal(QUEUED), delegateTaskQuery.criteria("status").equal(STARTED));
    wingsPersistence.update(
        delegateTaskQuery, wingsPersistence.createUpdateOperations(DelegateTask.class).set("status", ABORTED));

    broadcasterFactory.lookup("/stream/delegate/" + accountId, true)
        .broadcast(aDelegateTaskAbortEvent().withAccountId(accountId).withDelegateTaskId(delegateTaskId).build());
  }

  @Override
  public List<DelegateTaskEvent> getDelegateTaskEvents(String accountId, String delegateId, boolean syncOnly) {
    List<DelegateTaskEvent> delegateTaskEvents = new ArrayList<>(getQueuedEvents(true));
    if (!syncOnly) {
      delegateTaskEvents.addAll(getQueuedEvents(false));
      delegateTaskEvents.addAll(getAbortedEvents(delegateId));
    }

    logger.info("Dispatched delegateTaskIds:{} to delegate:[{}]",
        Joiner.on(",").join(delegateTaskEvents.stream().map(DelegateTaskEvent::getDelegateTaskId).collect(toList())),
        delegateId);

    return delegateTaskEvents;
  }

  private List<DelegateTaskEvent> getQueuedEvents(boolean sync) {
    return wingsPersistence.createQuery(DelegateTask.class)
        .filter("status", QUEUED)
        .filter("async", !sync)
        .field("delegateId")
        .doesNotExist()
        .project("accountId", true)
        .asList()
        .stream()
        .map(delegateTask
            -> aDelegateTaskEvent()
                   .withAccountId(delegateTask.getAccountId())
                   .withDelegateTaskId(delegateTask.getUuid())
                   .withSync(sync)
                   .build())
        .collect(toList());
  }

  private List<DelegateTaskEvent> getAbortedEvents(String delegateId) {
    Query<DelegateTask> abortedQuery = wingsPersistence.createQuery(DelegateTask.class)
                                           .filter("status", ABORTED)
                                           .filter("async", true)
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
  public void deleteOldTasks(long retentionMillis) {
    final int batchSize = 1000;
    final int limit = 5000;
    final long hours = TimeUnit.HOURS.convert(retentionMillis, TimeUnit.MILLISECONDS);
    try {
      logger.info("Start: Deleting delegate tasks older than {} hours", hours);
      timeLimiter.callWithTimeout(() -> {
        while (true) {
          List<Key<DelegateTask>> delegateTaskKeys = new ArrayList<>();
          try {
            Query<DelegateTask> query = wingsPersistence.createQuery(DelegateTask.class)
                                            .field("createdAt")
                                            .lessThan(clock.millis() - retentionMillis);
            delegateTaskKeys.addAll(query.asKeyList(new FindOptions().limit(limit).batchSize(batchSize)));
            if (isEmpty(delegateTaskKeys)) {
              logger.info("No more delegate tasks older than {} hours", hours);
              return true;
            }
            logger.info("Deleting {} delegate tasks", delegateTaskKeys.size());
            wingsPersistence.getCollection("delegateTasks")
                .remove(new BasicDBObject(ID_KEY,
                    new BasicDBObject("$in", delegateTaskKeys.stream().map(key -> key.getId().toString()).toArray())));
          } catch (Exception ex) {
            logger.warn("Failed to delete {} delegate tasks", delegateTaskKeys.size(), ex);
          }
          logger.info("Successfully deleted {} delegate tasks", delegateTaskKeys.size());
          if (delegateTaskKeys.size() < limit) {
            return true;
          }
          sleep(ofSeconds(2L));
        }
      }, 10L, TimeUnit.MINUTES, true);
    } catch (Exception ex) {
      logger.warn("Failed to delete delegate tasks older than {} hours within 10 minutes.", hours, ex);
    }
    logger.info("Deleted delegate tasks older than {} hours", hours);
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
}
