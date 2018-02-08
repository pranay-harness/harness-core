package software.wings.service.impl;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Status.ABORTED;
import static software.wings.beans.DelegateTask.Status.ERROR;
import static software.wings.beans.DelegateTask.Status.QUEUED;
import static software.wings.beans.DelegateTaskAbortEvent.Builder.aDelegateTaskAbortEvent;
import static software.wings.beans.DelegateTaskEvent.DelegateTaskEventBuilder.aDelegateTaskEvent;
import static software.wings.beans.ErrorCode.UNAVAILABLE_DELEGATES;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.alert.AlertType.NoEligibleDelegates;
import static software.wings.beans.alert.NoEligibleDelegatesAlert.NoEligibleDelegatesAlertBuilder.aNoEligibleDelegatesAlert;
import static software.wings.common.Constants.DELEGATE_SYNC_CACHE;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.exception.WingsException.ALERTING;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.zafarkhaja.semver.Version;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.mongodb.BasicDBObject;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.compress.archivers.zip.AsiExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Request;
import org.atmosphere.cpr.BroadcasterFactory;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
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
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
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
import software.wings.utils.CacheHelper;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.cache.Cache;
import javax.cache.Caching;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
@Singleton
@ValidateOnExecution
public class DelegateServiceImpl implements DelegateService {
  private static final Configuration cfg = new Configuration(VERSION_2_3_23);

  static {
    cfg.setTemplateLoader(new ClassTemplateLoader(DelegateServiceImpl.class, "/delegatetemplates"));
  }

  private static final Logger logger = LoggerFactory.getLogger(DelegateServiceImpl.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private AccountService accountService;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private EventEmitter eventEmitter;
  @Inject private HazelcastInstance hazelcastInstance;
  @Inject private BroadcasterFactory broadcasterFactory;
  @Inject private CacheHelper cacheHelper;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private AlertService alertService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private Clock clock;

  @Override
  public PageResponse<Delegate> list(PageRequest<Delegate> pageRequest) {
    return wingsPersistence.query(Delegate.class, pageRequest);
  }

  @Override
  public Delegate get(String accountId, String delegateId) {
    return wingsPersistence.createQuery(Delegate.class)
        .field("accountId")
        .equal(accountId)
        .field(Mapper.ID_KEY)
        .equal(delegateId)
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

    logger.info("Updating delegate : {}", delegate.getUuid());
    return updateDelegate(delegate, updateOperations);
  }

  @Override
  public Delegate updateHeartbeat(String accountId, String delegateId) {
    wingsPersistence.update(wingsPersistence.createQuery(Delegate.class)
                                .field("accountId")
                                .equal(accountId)
                                .field(ID_KEY)
                                .equal(delegateId),
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
                                .field("accountId")
                                .equal(delegate.getAccountId())
                                .field(ID_KEY)
                                .equal(delegate.getUuid()),
        updateOperations);

    // Touch currently executing tasks.
    if (delegate.getCurrentlyExecutingDelegateTasks() != null
        && isNotEmpty(delegate.getCurrentlyExecutingDelegateTasks())) {
      logger.info("Updating tasks");

      Query<DelegateTask> delegateTaskQuery =
          wingsPersistence.createQuery(DelegateTask.class)
              .field("accountId")
              .equal(delegate.getAccountId())
              .field("delegateId")
              .equal(delegate.getUuid())
              .field("status")
              .equal(DelegateTask.Status.STARTED)
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
    if (MapUtils.isNotEmpty(scriptParams)) {
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
        cfg.getTemplate("stop.sh.ftl").process(null, stringWriter);
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

    String watcherLatestVersion = "";
    String watcherJarRelativePath;
    String watcherJarDownloadUrl = "";
    String watcherMetadataUrl = "";

    try {
      String delegateMetadataUrl = mainConfiguration.getDelegateMetadataUrl().trim();
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

    if (doUpgrade) {
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
          .build();
    }
    return null;
  }

  @Override
  public File download(String managerHost, String accountId) throws IOException, TemplateException {
    File delegateFile = File.createTempFile(Constants.DELEGATE_DIR, ".zip");

    ZipArchiveOutputStream out = new ZipArchiveOutputStream(delegateFile);
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
      cfg.getTemplate("stop.sh.ftl").process(null, fileWriter);
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
    out.closeArchiveEntry();

    out.close();
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
  public void delete(String accountId, String delegateId) {
    logger.info("Deleting delegate: {}", delegateId);
    wingsPersistence.delete(wingsPersistence.createQuery(Delegate.class)
                                .field("accountId")
                                .equal(accountId)
                                .field(ID_KEY)
                                .equal(delegateId));
  }

  @Override
  public Delegate register(Delegate delegate) {
    logger.info("Registering delegate for account {}: Hostname: {} IP: {}", delegate.getAccountId(),
        delegate.getHostName(), delegate.getIp());
    // Please do not remove Fields Excluded
    Delegate existingDelegate = wingsPersistence.get(Delegate.class,
        aPageRequest()
            .addFilter("ip", EQ, delegate.getIp())
            .addFilter("hostName", EQ, delegate.getHostName())
            .addFilter("accountId", EQ, delegate.getAccountId())
            .addFieldsExcluded("supportedTaskTypes")
            .build());
    Delegate registeredDelegate;
    if (existingDelegate == null) {
      logger.info("No existing delegate, adding: {}", delegate.getUuid());
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
  public PageResponse<DelegateTask> getDelegateTasks(String accountId, String delegateId) {
    return wingsPersistence.query(DelegateTask.class, aPageRequest().addFilter("accountId", EQ, accountId).build());
  }

  @Override
  public String queueTask(DelegateTask task) {
    logger.info("Queueing task uuid: {}, accountId: {}, type: {}, async: {}", task.getUuid(), task.getAccountId(),
        task.getTaskType(), task.isAsync());
    wingsPersistence.save(task);
    broadcasterFactory.lookup("/stream/delegate/" + task.getAccountId(), true).broadcast(task);
    return task.getUuid();
  }

  @Override
  public <T extends NotifyResponseData> T executeTask(DelegateTask task) throws InterruptedException {
    List<String> eligibleDelegateIds = ensureDelegateAvailableToExecuteTask(task);
    if (isEmpty(eligibleDelegateIds)) {
      throw new WingsException(UNAVAILABLE_DELEGATES, ALERTING);
    }
    String taskId = UUIDGenerator.getUuid();
    task.setQueueName(taskId);
    task.setUuid(taskId);
    task.setCreatedAt(clock.millis());
    IQueue<T> topic = hazelcastInstance.getQueue(taskId);
    cacheHelper.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class).put(taskId, task);
    broadcasterFactory.lookup("/stream/delegate/" + task.getAccountId(), true).broadcast(task);
    logger.info("Broadcast new task: uuid: {}, accountId: {}, type: {}, async: {}", task.getUuid(), task.getAccountId(),
        task.getTaskType(), task.isAsync());
    T responseData = topic.poll(task.getTimeout(), TimeUnit.MILLISECONDS);
    if (responseData == null) {
      logger.warn("Task {} timed out. remove it from cache", task.getUuid());
      Caching.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class).remove(taskId);
      throw new WingsException(ErrorCode.REQUEST_TIMEOUT).addParam("name", Constants.DELEGATE_NAME);
    }
    return responseData;
  }

  private List<String> ensureDelegateAvailableToExecuteTask(DelegateTask task) {
    if (task == null) {
      logger.warn("Delegate task is null");
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Delegate task is null");
    }
    List<Key<Delegate>> activeDelegates = wingsPersistence.createQuery(Delegate.class)
                                              .field("accountId")
                                              .equal(task.getAccountId())
                                              .field("connected")
                                              .equal(true)
                                              .field("status")
                                              .equal(Status.ENABLED)
                                              .field("supportedTaskTypes")
                                              .contains(task.getTaskType().name())
                                              .field("lastHeartBeat")
                                              .greaterThanOrEq(System.currentTimeMillis() - 3 * 60 * 1000)
                                              .asKeyList(); // TODO:: make it more reliable. take out time factor

    logger.info("{} delegates [{}] are active", activeDelegates.size(),
        activeDelegates.stream().map(delegateKey -> delegateKey.getId().toString()).collect(Collectors.joining(", ")));

    List<Key<Delegate>> eligibleDelegates =
        activeDelegates.stream()
            .filter(delegateKey -> assignDelegateService.canAssign(delegateKey.getId().toString(), task))
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

    List<String> eligibleDelegateIds =
        eligibleDelegates.stream().map(delegateKey -> delegateKey.getId().toString()).collect(Collectors.toList());
    logger.info("{} delegates [{}] eligible to execute the task", eligibleDelegates.size(),
        Joiner.on(", ").join(eligibleDelegateIds));
    return eligibleDelegateIds;
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
    if (isBlacklisted(delegateId, delegateTask)) {
      logger.info("Delegate {} is blacklisted for task {}", delegateId, taskId);
      return null;
    }

    if (assignDelegateService.isWhitelisted(delegateTask, delegateId)) {
      return assignTask(delegateId, taskId, delegateTask);
    } else {
      // Set delegate as validating
      addToValidating(delegateId, delegateTask);
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

    // Mark delegate as validation complete
    addToValidationComplete(delegateId, delegateTask);
    if (results.stream().anyMatch(DelegateConnectionResult::isValidated)) {
      return assignTask(delegateId, taskId, delegateTask);
    } else if (clock.millis() - delegateTask.getCreatedAt() > TimeUnit.MINUTES.toMillis(5)) {
      addToBlacklisted(delegateId, delegateTask);
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

    // Tell delegate whether to proceed anyway because all eligible delegates failed
    if (isValidationComplete(delegateTask)) {
      logger.info("Validation attempts are complete for task {}", taskId);
      return assignTask(delegateId, taskId, delegateTask);
    } else {
      logger.info("Task {} is still being validated");
      return null;
    }
  }

  private void addToValidating(String delegateId, DelegateTask delegateTask) {
    logger.info("Delegate {} to validate task {} {}", delegateId, delegateTask.getUuid(),
        delegateTask.isAsync() ? "(async)" : "(sync)");
    Set<String> validating = Optional.ofNullable(delegateTask.getValidatingDelegateIds()).orElse(new HashSet<>());
    validating.add(delegateId);
    delegateTask.setValidatingDelegateIds(validating);
    storeDelegateTracking(delegateTask, "validatingDelegateIds", delegateTask.getValidatingDelegateIds());
  }

  private void addToValidationComplete(String delegateId, DelegateTask delegateTask) {
    logger.info("Delegate {} completed validating task {} {}", delegateId, delegateTask.getUuid(),
        delegateTask.isAsync() ? "(async)" : "(sync)");
    Set<String> validationComplete =
        Optional.ofNullable(delegateTask.getValidationCompleteDelegateIds()).orElse(new HashSet<>());
    validationComplete.add(delegateId);
    delegateTask.setValidationCompleteDelegateIds(validationComplete);
    storeDelegateTracking(
        delegateTask, "validationCompleteDelegateIds", delegateTask.getValidationCompleteDelegateIds());
  }

  private void addToBlacklisted(String delegateId, DelegateTask delegateTask) {
    logger.info("Delegate {} blacklisted for task {} {}", delegateId, delegateTask.getUuid(),
        delegateTask.isAsync() ? "(async)" : "(sync)");
    Set<String> blacklisted = Optional.ofNullable(delegateTask.getBlacklistedDelegateIds()).orElse(new HashSet<>());
    blacklisted.add(delegateId);
    delegateTask.setBlacklistedDelegateIds(blacklisted);
    storeDelegateTracking(delegateTask, "blacklistedDelegateIds", delegateTask.getBlacklistedDelegateIds());
  }

  private boolean isValidationComplete(DelegateTask delegateTask) {
    Set<String> validatingDelegates = delegateTask.getValidatingDelegateIds();
    Set<String> completeDelegates = delegateTask.getValidationCompleteDelegateIds();
    return isNotEmpty(validatingDelegates) && isNotEmpty(completeDelegates)
        && completeDelegates.containsAll(validatingDelegates);
  }

  private boolean isBlacklisted(String delegateId, DelegateTask delegateTask) {
    Set<String> blacklistedDelegateIds = delegateTask.getBlacklistedDelegateIds();
    return isNotEmpty(blacklistedDelegateIds) && blacklistedDelegateIds.contains(delegateId);
  }

  private void storeDelegateTracking(
      DelegateTask delegateTask, String trackDelegateField, Set<String> trackedDelegates) {
    if (delegateTask.isAsync()) {
      UpdateOperations<DelegateTask> updateOperations =
          wingsPersistence.createUpdateOperations(DelegateTask.class).set(trackDelegateField, trackedDelegates);
      Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                            .field("accountId")
                                            .equal(delegateTask.getAccountId())
                                            .field("status")
                                            .equal(QUEUED)
                                            .field("delegateId")
                                            .doesNotExist()
                                            .field(ID_KEY)
                                            .equal(delegateTask.getUuid());
      wingsPersistence.update(updateQuery, updateOperations);
    } else {
      Caching.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class).put(delegateTask.getUuid(), delegateTask);
    }
  }

  private void clearFromValidationCache(DelegateTask delegateTask) {
    delegateTask.setValidatingDelegateIds(null);
    delegateTask.setValidationCompleteDelegateIds(null);
    delegateTask.setBlacklistedDelegateIds(null);
    if (delegateTask.isAsync()) {
      UpdateOperations<DelegateTask> updateOperations = wingsPersistence.createUpdateOperations(DelegateTask.class)
                                                            .unset("validatingDelegateIds")
                                                            .unset("validationCompleteDelegateIds")
                                                            .unset("blacklistedDelegateIds");
      Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                            .field("accountId")
                                            .equal(delegateTask.getAccountId())
                                            .field("status")
                                            .equal(QUEUED)
                                            .field("delegateId")
                                            .doesNotExist()
                                            .field(ID_KEY)
                                            .equal(delegateTask.getUuid());
      wingsPersistence.update(updateQuery, updateOperations);
    } else {
      Caching.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class).put(delegateTask.getUuid(), delegateTask);
    }
  }

  private DelegateTask getUnassignedDelegateTask(String accountId, String taskId) {
    DelegateTask delegateTask = cacheHelper.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class).get(taskId);
    if (delegateTask != null) {
      // Sync
      logger.info("Got delegate task from cache: {}", delegateTask.getUuid());
      if (isNotBlank(delegateTask.getDelegateId())) {
        logger.info("Task {} is already assigned to delegate {}", taskId, delegateTask.getDelegateId());
        delegateTask = null;
      }
    } else {
      logger.info("Delegate task {} not in cache, checking database.", taskId);
      // Async
      delegateTask = wingsPersistence.createQuery(DelegateTask.class)
                         .field("accountId")
                         .equal(accountId)
                         .field("status")
                         .equal(QUEUED)
                         .field("delegateId")
                         .doesNotExist()
                         .field(ID_KEY)
                         .equal(taskId)
                         .get();
      if (delegateTask != null) {
        logger.info("Delegate task from database: {}", delegateTask.getUuid());
        delegateTask.setAsync(true);
      } else {
        logger.warn("Delegate task {} is already assigned", taskId);
      }
    }
    return delegateTask;
  }

  private DelegateTask assignTask(String delegateId, String taskId, DelegateTask delegateTask) {
    // Clear pending validations. No longer need to track since we're assigning.
    clearFromValidationCache(delegateTask);

    logger.info(
        "Assigning task {} to delegate {} {}", taskId, delegateId, delegateTask.isAsync() ? "(async)" : "(sync)");
    delegateTask.setDelegateId(delegateId);
    if (delegateTask.isAsync()) {
      UpdateOperations<DelegateTask> updateOperations =
          wingsPersistence.createUpdateOperations(DelegateTask.class).set("delegateId", delegateId);
      Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                            .field("accountId")
                                            .equal(delegateTask.getAccountId())
                                            .field("status")
                                            .equal(QUEUED)
                                            .field("delegateId")
                                            .doesNotExist()
                                            .field(ID_KEY)
                                            .equal(taskId);
      UpdateResults updateResults = wingsPersistence.update(updateQuery, updateOperations);
      if (updateResults.getUpdatedCount() == 0) {
        // Couldn't assign, probably because it was already assigned to another delegate.
        delegateTask = null;
      }
    } else {
      Caching.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class).put(taskId, delegateTask);
    }
    return delegateTask;
  }

  @Override
  public DelegateTask startDelegateTask(String accountId, String delegateId, String taskId) {
    logger.info("Starting task {} with delegate {}", taskId, delegateId);
    DelegateTask delegateTask = cacheHelper.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class).get(taskId);
    if (delegateTask == null) {
      logger.info("Delegate task from cache is null for task {}", taskId);
      Query<DelegateTask> query = wingsPersistence.createQuery(DelegateTask.class)
                                      .field("accountId")
                                      .equal(accountId)
                                      .field("status")
                                      .equal(QUEUED)
                                      .field("delegateId")
                                      .equal(delegateId)
                                      .field(ID_KEY)
                                      .equal(taskId);
      UpdateOperations<DelegateTask> updateOperations =
          wingsPersistence.createUpdateOperations(DelegateTask.class).set("status", DelegateTask.Status.STARTED);
      delegateTask = wingsPersistence.getDatastore().findAndModify(query, updateOperations);
    } else {
      logger.info("Removing delegate task from cache: {}", delegateTask.getUuid());
      Caching.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class).remove(taskId);
    }
    return delegateTask;
  }

  @Override
  public void clearCache(String delegateId) {
    assignDelegateService.clearConnectionResults(delegateId);
  }

  @Override
  public void processDelegateResponse(DelegateTaskResponse response) {
    DelegateTask task = response.getTask();
    logger.info("Delegate [{}], response received for task [{}, {}, {}]", task.getDelegateId(), task.getUuid(),
        task.getStatus(), task.getTaskType());
    if (task.isAsync()) {
      DelegateTask delegateTask = wingsPersistence.get(DelegateTask.class,
          aPageRequest()
              .addFilter("accountId", EQ, response.getAccountId())
              .addFilter(ID_KEY, EQ, task.getUuid())
              .build());
      if (delegateTask != null) {
        String waitId = delegateTask.getWaitId();
        if (waitId != null) {
          waitNotifyEngine.notify(waitId, response.getResponse());
        } else {
          logger.error("Async task {} with type {} has no wait ID", task.getUuid(), task.getTaskType().name());
        }
        wingsPersistence.delete(wingsPersistence.createQuery(DelegateTask.class)
                                    .field("accountId")
                                    .equal(response.getAccountId())
                                    .field(ID_KEY)
                                    .equal(delegateTask.getUuid()));
      }
    } else {
      String topicName = task.getQueueName();
      if (isNotBlank(topicName)) {
        IQueue<NotifyResponseData> topic = hazelcastInstance.getQueue(topicName);
        boolean queued = topic.offer(response.getResponse());
        logger.info(
            "Sync call response added to queue [name: {}, object: {}] with status [{}]", topicName, topic, queued);
      } else {
        logger.error("Sync task {} with type {} has no queue name", task.getUuid(), task.getTaskType().name());
      }
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
    DelegateTask updatedTask =
        wingsPersistence.getDatastore().findAndModify(wingsPersistence.createQuery(DelegateTask.class)
                                                          .field(ID_KEY)
                                                          .equal(delegateTaskId)
                                                          .field("accountId")
                                                          .equal(accountId)
                                                          .field("status")
                                                          .equal(QUEUED),
            wingsPersistence.createUpdateOperations(DelegateTask.class).set("status", ABORTED).unset("delegateId"));

    if (updatedTask == null) {
      logger.info("Updated task null");
      broadcasterFactory.lookup("/stream/delegate/" + accountId, true)
          .broadcast(aDelegateTaskAbortEvent().withAccountId(accountId).withDelegateTaskId(delegateTaskId).build());
    }
  }

  @Override
  public List<DelegateTaskEvent> getDelegateTaskEvents(String accountId, String delegateId, boolean syncOnly) {
    List<DelegateTaskEvent> delegateTaskEvents = new ArrayList<>(getSyncEvents());
    if (!syncOnly) {
      delegateTaskEvents.addAll(getQueuedEvents());
      delegateTaskEvents.addAll(getAbortedEvents(delegateId));
    }

    logger.info("Dispatched delegateTaskIds:{} to delegate:[{}]",
        Joiner.on(",").join(delegateTaskEvents.stream().map(DelegateTaskEvent::getDelegateTaskId).collect(toList())),
        delegateId);

    return delegateTaskEvents;
  }

  private List<DelegateTaskEvent> getSyncEvents() {
    List<DelegateTaskEvent> syncTaskEvents = new ArrayList<>();
    Cache<String, DelegateTask> delegateSyncCache =
        cacheHelper.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class);
    Iterator<Cache.Entry<String, DelegateTask>> iterator = delegateSyncCache.iterator();
    try {
      while (iterator.hasNext()) {
        Cache.Entry<String, DelegateTask> stringDelegateTaskEntry = iterator.next();
        if (stringDelegateTaskEntry != null) {
          try {
            DelegateTask syncDelegateTask = stringDelegateTaskEntry.getValue();
            if (syncDelegateTask.getStatus().equals(QUEUED) && syncDelegateTask.getDelegateId() == null) {
              syncTaskEvents.add(aDelegateTaskEvent()
                                     .withAccountId(syncDelegateTask.getAccountId())
                                     .withDelegateTaskId(syncDelegateTask.getUuid())
                                     .withSync(!syncDelegateTask.isAsync())
                                     .build());
            }
          } catch (Exception ex) {
            logger.error("Could not fetch delegate task from queue", ex);
            logger.warn("Remove Delegate task {} from cache", stringDelegateTaskEntry.getKey());
            Caching.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class)
                .remove(stringDelegateTaskEntry.getKey());
          }
        }
      }
    } catch (Exception e) {
      delegateSyncCache.clear();
    }
    return syncTaskEvents;
  }

  private List<DelegateTaskEvent> getQueuedEvents() {
    Query<DelegateTask> queuedQuery = wingsPersistence.createQuery(DelegateTask.class)
                                          .field("status")
                                          .equal(QUEUED)
                                          .field("delegateId")
                                          .doesNotExist()
                                          .project("accountId", true)
                                          .project("status", true)
                                          .project("async", true);
    return queuedQuery.asList()
        .stream()
        .map(delegateTask
            -> aDelegateTaskEvent()
                   .withAccountId(delegateTask.getAccountId())
                   .withDelegateTaskId(delegateTask.getUuid())
                   .withSync(!delegateTask.isAsync())
                   .build())
        .collect(toList());
  }

  private List<DelegateTaskEvent> getAbortedEvents(String delegateId) {
    Query<DelegateTask> abortedQuery = wingsPersistence.createQuery(DelegateTask.class)
                                           .field("status")
                                           .equal(ABORTED)
                                           .field("delegateId")
                                           .equal(delegateId);

    List<DelegateTaskEvent> delegateTaskAbortEvents = abortedQuery.project("accountId", true)
                                                          .project("status", true)
                                                          .project("async", true)
                                                          .asList()
                                                          .stream()
                                                          .map(delegateTask
                                                              -> aDelegateTaskAbortEvent()
                                                                     .withAccountId(delegateTask.getAccountId())
                                                                     .withDelegateTaskId(delegateTask.getUuid())
                                                                     .withSync(!delegateTask.isAsync())
                                                                     .build())
                                                          .collect(toList());
    UpdateOperations<DelegateTask> updateOperations =
        wingsPersistence.createUpdateOperations(DelegateTask.class).unset("delegateId");
    wingsPersistence.update(abortedQuery, updateOperations);
    return delegateTaskAbortEvents;
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
            query.or(query.criteria("status").equal(ABORTED), query.criteria("status").equal(ERROR));
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
}
