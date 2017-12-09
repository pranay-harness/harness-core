package software.wings.service.impl;

import static com.google.common.collect.Iterables.isEmpty;
import static freemarker.template.Configuration.VERSION_2_3_23;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTaskAbortEvent.Builder.aDelegateTaskAbortEvent;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.alert.AlertType.NoEligibleDelegates;
import static software.wings.beans.alert.NoEligibleDelegatesAlert.NoEligibleDelegatesAlertBuilder.aNoEligibleDelegatesAlert;
import static software.wings.common.Constants.DELEGATE_SYNC_CACHE;
import static software.wings.common.Constants.DELEGATE_VALIDATION_CACHE;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import com.github.zafarkhaja.semver.Version;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.compress.archivers.zip.AsiExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Request;
import org.atmosphere.cpr.BroadcasterFactory;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.Mapper;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.cache.Cache;
import javax.cache.Caching;
import javax.inject.Inject;
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

  private final Logger logger = LoggerFactory.getLogger(getClass());
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
        && !isEmpty(delegate.getCurrentlyExecutingDelegateTasks())) {
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
    Delegate delegate = get(accountId, delegateId);
    logger.info("Checking delegate for upgrade: {}", delegate.getUuid());

    ImmutableMap<Object, Object> scriptParams = getJarAndScriptRunTimeParamMap(accountId, version, managerHost);

    DelegateScripts delegateScripts = new DelegateScripts();
    delegateScripts.setDelegateId(delegateId);
    delegateScripts.setVersion(version);
    delegateScripts.setDoUpgrade(false);
    if (scriptParams != null && scriptParams.size() > 0) {
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
      delegateJarDownloadUrl = "http://" + (delegateMetadataUrl.split("/")[2]).trim() + "/" + jarRelativePath;
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
      watcherJarDownloadUrl = "http://" + (watcherMetadataUrl.split("/")[2]).trim() + "/" + watcherJarRelativePath;
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
      throw new WingsException(ErrorCode.UNAVAILABLE_DELEGATES);
    }
    String taskId = UUIDGenerator.getUuid();
    task.setQueueName(taskId);
    task.setUuid(taskId);
    IQueue<T> topic = hazelcastInstance.getQueue(taskId);
    cacheHelper.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class).put(taskId, task);
    broadcasterFactory.lookup("/stream/delegate/" + task.getAccountId(), true).broadcast(task);
    logger.info("Broadcast new task: uuid: {}, accountId: {}, type: {}, async: {}", task.getUuid(), task.getAccountId(),
        task.getTaskType(), task.isAsync());
    T responseData = topic.poll(task.getTimeout(), TimeUnit.MILLISECONDS);
    if (responseData == null) {
      logger.warn("Task [{}] timed out. remove it from cache", task.toString());
      Caching.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class).remove(taskId);
      throw new WingsException(ErrorCode.REQUEST_TIMEOUT, "name", Constants.DELEGATE_NAME);
    }
    return responseData;
  }

  private List<String> ensureDelegateAvailableToExecuteTask(DelegateTask task) {
    if (task == null) {
      logger.warn("Delegate task is null");
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", "Delegate task is null");
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

    if (activeDelegates.size() == 0) {
      logger.warn("No delegates are active for the account: {}", task.getAccountId());
      alertService.openAlert(task.getAccountId(), GLOBAL_APP_ID, AlertType.NoActiveDelegates,
          NoActiveDelegatesAlert.builder().accountId(task.getAccountId()).build());
    } else if (eligibleDelegates.size() == 0) {
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
    if (delegateTask != null && !assignDelegateService.canAssign(delegateId, delegateTask)) {
      logger.info("Delegate {} is not scoped for task {}", delegateId, taskId);
      ensureDelegateAvailableToExecuteTask(delegateTask); // Raises an alert if there are no eligible delegates.
      delegateTask = null;
    }
    if (delegateTask != null
        && Optional.ofNullable(delegateTask.getBlacklistedDelegateIds()).orElse(emptyList()).contains(delegateId)) {
      logger.info("Delegate {} is blacklisted for task {}", delegateId, taskId);
      delegateTask = null;
    }
    if (delegateTask != null) {
      if (assignDelegateService.isWhitelisted(delegateTask, delegateId)) {
        return assignTask(delegateId, taskId, delegateTask);
      } else {
        logger.info(
            "Delegate {} to validate task {} {}", delegateId, taskId, delegateTask.isAsync() ? "(async)" : "(sync)");
        // Store delegateId as validation response pending
        addToValidationCache(delegateId, taskId);
      }
    }
    return delegateTask;
  }

  @Override
  public DelegateTask reportConnectionResults(
      String accountId, String delegateId, String taskId, List<DelegateConnectionResult> results) {
    // Remove delegateId from pending validation responses
    markCompleteInValidationCache(delegateId, taskId);

    assignDelegateService.saveConnectionResults(results);

    DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId);
    if (delegateTask != null) {
      if (results.stream().anyMatch(DelegateConnectionResult::isValidated)) {
        return assignTask(delegateId, taskId, delegateTask);
      } else {
        blacklistDelegateForTask(delegateId, delegateTask);
      }
    }
    return null;
  }

  private void blacklistDelegateForTask(String delegateId, DelegateTask delegateTask) {
    logger.info("Blacklisting delegate {} for task {}", delegateId, delegateTask.getUuid());
    List<String> blacklistedDelegates =
        Optional.ofNullable(delegateTask.getBlacklistedDelegateIds()).orElse(new ArrayList<>());
    blacklistedDelegates.add(delegateId);
    if (delegateTask.isAsync()) {
      UpdateOperations<DelegateTask> updateOperations = wingsPersistence.createUpdateOperations(DelegateTask.class)
                                                            .set("blacklistedDelegateIds", blacklistedDelegates);
      Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                            .field("accountId")
                                            .equal(delegateTask.getAccountId())
                                            .field("status")
                                            .equal(DelegateTask.Status.QUEUED)
                                            .field("delegateId")
                                            .doesNotExist()
                                            .field(ID_KEY)
                                            .equal(delegateTask.getUuid());
      wingsPersistence.update(updateQuery, updateOperations);
    } else {
      delegateTask.setBlacklistedDelegateIds(blacklistedDelegates);
      Caching.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class).put(delegateTask.getUuid(), delegateTask);
    }
  }

  @Override
  public DelegateTask shouldProceedAnyway(String accountId, String delegateId, String taskId) {
    // Tell delegate whether to proceed anyway because all eligible delegates failed.
    if (isValidationComplete(taskId)) {
      logger.info("Validation attempts are complete for task {}", taskId);
      DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId);
      if (delegateTask != null) {
        return assignTask(delegateId, taskId, delegateTask);
      } else {
        logger.info("Task {} was already assigned", taskId);
      }
    } else {
      logger.info("Task {} is still being validated");
    }
    return null;
  }

  private void addToValidationCache(String delegateId, String taskId) {
    Cache<String, Set> delegateValidationCache =
        cacheHelper.getCache(DELEGATE_VALIDATION_CACHE, String.class, Set.class);
    Set<String> validatingDelegates = delegateValidationCache.get("validating-" + taskId);
    if (validatingDelegates == null) {
      validatingDelegates = new HashSet<>();
    }
    validatingDelegates.add(delegateId);
    delegateValidationCache.put("validating-" + taskId, validatingDelegates);
  }

  private void markCompleteInValidationCache(String delegateId, String taskId) {
    Cache<String, Set> delegateValidationCache =
        cacheHelper.getCache(DELEGATE_VALIDATION_CACHE, String.class, Set.class);
    Set<String> completeDelegates = delegateValidationCache.get("complete-" + taskId);
    if (completeDelegates == null) {
      completeDelegates = new HashSet<>();
    }
    completeDelegates.add(delegateId);
    delegateValidationCache.put("complete-" + taskId, completeDelegates);
  }

  private boolean isValidationComplete(String taskId) {
    Cache<String, Set> delegateValidationCache =
        cacheHelper.getCache(DELEGATE_VALIDATION_CACHE, String.class, Set.class);
    Set<String> validatingDelegates = delegateValidationCache.get("validating-" + taskId);
    Set<String> completeDelegates = delegateValidationCache.get("complete-" + taskId);
    return validatingDelegates != null && completeDelegates != null
        && completeDelegates.containsAll(validatingDelegates);
  }

  private void clearFromValidationCache(String taskId) {
    cacheHelper.getCache(DELEGATE_VALIDATION_CACHE, String.class, Set.class).remove("validating-" + taskId);
    cacheHelper.getCache(DELEGATE_VALIDATION_CACHE, String.class, Set.class).remove("complete-" + taskId);
  }

  private DelegateTask getUnassignedDelegateTask(String accountId, String taskId) {
    DelegateTask delegateTask = cacheHelper.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class).get(taskId);
    if (delegateTask != null) {
      // Sync
      logger.info("Delegate task from cache: {}", delegateTask.getUuid());
      if (!isBlank(delegateTask.getDelegateId())) {
        logger.info("Task {} is already assigned to delegate {}", taskId, delegateTask.getDelegateId());
        delegateTask = null;
      }
    } else {
      // Async
      delegateTask = wingsPersistence.createQuery(DelegateTask.class)
                         .field("accountId")
                         .equal(accountId)
                         .field("status")
                         .equal(DelegateTask.Status.QUEUED)
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
    clearFromValidationCache(taskId);

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
                                            .equal(DelegateTask.Status.QUEUED)
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
                                      .equal(DelegateTask.Status.QUEUED)
                                      .field("delegateId")
                                      .equal(delegateId)
                                      .field(ID_KEY)
                                      .equal(taskId);
      UpdateOperations<DelegateTask> updateOperations =
          wingsPersistence.createUpdateOperations(DelegateTask.class).set("status", DelegateTask.Status.STARTED);
      delegateTask = wingsPersistence.getDatastore().findAndModify(query, updateOperations);
    } else {
      logger.info("Delegate task from cache: {}", delegateTask.getUuid());
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
    logger.info("Delegate [{}], response received for task [{}, {}, {}]", response.getTask().getDelegateId(),
        response.getTask().getUuid(), response.getTask().getStatus(), response.getTask().getTaskType());
    if (isNotBlank(response.getTask().getWaitId())) {
      DelegateTask delegateTask = wingsPersistence.get(DelegateTask.class,
          aPageRequest()
              .addFilter("accountId", EQ, response.getAccountId())
              .addFilter(ID_KEY, EQ, response.getTask().getUuid())
              .build());
      if (delegateTask != null) {
        String waitId = delegateTask.getWaitId();
        waitNotifyEngine.notify(waitId, response.getResponse());
        wingsPersistence.delete(wingsPersistence.createQuery(DelegateTask.class)
                                    .field("accountId")
                                    .equal(response.getAccountId())
                                    .field(ID_KEY)
                                    .equal(delegateTask.getUuid()));
      }
    } else {
      String topicName = response.getTask().getQueueName();
      // do the haze
      IQueue<NotifyResponseData> topic = hazelcastInstance.getQueue(topicName);
      boolean queued = topic.offer(response.getResponse());
      logger.info(
          "Sync call response added to queue [name: {}, object: {}] with status [{}]", topicName, topic, queued);
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
                                                          .equal(DelegateTask.Status.QUEUED),
            wingsPersistence.createUpdateOperations(DelegateTask.class)
                .set("status", DelegateTask.Status.ABORTED)
                .unset("delegateId"));

    if (updatedTask == null) {
      logger.info("Updated task null");
      broadcasterFactory.lookup("/stream/delegate/" + accountId, true)
          .broadcast(aDelegateTaskAbortEvent().withAccountId(accountId).withDelegateTaskId(delegateTaskId).build());
    }
  }
}
