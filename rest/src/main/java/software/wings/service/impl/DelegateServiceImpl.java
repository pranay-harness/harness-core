package software.wings.service.impl;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import com.github.zafarkhaja.semver.Version;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.compress.archivers.zip.AsiExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Request;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.Event.Type;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
@Singleton
public class DelegateServiceImpl implements DelegateService {
  private static final Configuration cfg = new Configuration(VERSION_2_3_23);

  static {
    cfg.setTemplateLoader(new ClassTemplateLoader(DelegateServiceImpl.class, "/delegatetemplates"));
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private ExecutorService executorService;
  @Inject private AccountService accountService;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private EventEmitter eventEmitter;

  @Override
  public PageResponse<Delegate> list(PageRequest<Delegate> pageRequest) {
    return wingsPersistence.query(Delegate.class, pageRequest);
  }

  @Override
  public Delegate get(String accountId, String delegateId) {
    return wingsPersistence.get(
        Delegate.class, aPageRequest().addFilter("accountId", EQ, accountId).addFilter(ID_KEY, EQ, delegateId).build());
  }

  @Override
  public Delegate update(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = wingsPersistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, "status", delegate.getStatus());
    setUnset(updateOperations, "lastHeartBeat", delegate.getLastHeartBeat());

    wingsPersistence.update(wingsPersistence.createQuery(Delegate.class)
                                .field("accountId")
                                .equal(delegate.getAccountId())
                                .field(ID_KEY)
                                .equal(delegate.getUuid()),
        updateOperations);
    eventEmitter.send(Channel.DELEGATES,
        anEvent().withOrgId(delegate.getAccountId()).withUuid(delegate.getUuid()).withType(Type.UPDATE).build());
    Delegate updatedDelegate = get(delegate.getAccountId(), delegate.getUuid());
    return updatedDelegate;
  }

  @Override
  public Delegate checkForUpgrade(String accountId, String delegateId, String version, String managerHost)
      throws IOException, TemplateException {
    Delegate delegate = get(accountId, delegateId);

    String latestVersion = null;
    try {
      latestVersion = substringBefore(Request.Get(mainConfiguration.getDelegateMetadataUrl())
                                          .connectTimeout(1000)
                                          .socketTimeout(1000)
                                          .execute()
                                          .returnContent()
                                          .asString(),
          " ");
    } catch (IOException e) {
      logger.error("Unable to fetch delegate version information ", e);
      latestVersion = "0.0.0";
    }
    boolean doUpgrade = Version.valueOf(version).lessThan(Version.valueOf(latestVersion));

    delegate.setDoUpgrade(doUpgrade);
    if (doUpgrade) {
      delegate.setVersion(latestVersion);
      try (StringWriter stringWriter = new StringWriter()) {
        cfg.getTemplate("upgrade.sh.ftl")
            .process(ImmutableMap.of("delegateMetadataUrl", mainConfiguration.getDelegateMetadataUrl(), "accountId",
                         accountId, "managerHostAndPort", managerHost),
                stringWriter);
        delegate.setUpgradeScript(stringWriter.toString());
      }
    }
    return delegate;
  }

  @Override
  public Delegate add(Delegate delegate) {
    delegate.setAppId(GLOBAL_APP_ID);
    Delegate savedDelegate = wingsPersistence.saveAndGet(Delegate.class, delegate);
    eventEmitter.send(Channel.DELEGATES,
        anEvent().withOrgId(delegate.getAccountId()).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
    return delegate;
  }

  @Override
  public void delete(String accountId, String delegateId) {
    wingsPersistence.delete(wingsPersistence.createQuery(Delegate.class)
                                .field("accountId")
                                .equal(accountId)
                                .field(ID_KEY)
                                .equal(delegateId));
  }

  @Override
  public Delegate register(Delegate delegate) {
    Delegate existingDelegate = wingsPersistence.get(Delegate.class,
        aPageRequest()
            .addFilter("ip", EQ, delegate.getIp())
            .addFilter("hostName", EQ, delegate.getHostName())
            .addFilter("accountId", EQ, delegate.getAccountId())
            .build());

    if (existingDelegate == null) {
      return add(delegate);
    } else {
      delegate.setUuid(existingDelegate.getUuid());
      delegate.setStatus(existingDelegate.getStatus() == Status.DISABLED ? Status.DISABLED : delegate.getStatus());
      return update(delegate);
    }
  }

  @Override
  public void sendTaskWaitNotify(DelegateTask task) {
    wingsPersistence.save(task);
  }

  @Override
  public PageResponse<DelegateTask> getDelegateTasks(String accountId, String delegateId) {
    return wingsPersistence.query(DelegateTask.class, aPageRequest().addFilter("accountId", EQ, accountId).build());
  }

  @Override
  public void processDelegateResponse(DelegateTaskResponse response) {
    DelegateTask delegateTask = wingsPersistence.get(DelegateTask.class,
        aPageRequest()
            .addFilter("accountId", EQ, response.getAccountId())
            .addFilter(ID_KEY, EQ, response.getTaskId())
            .build());
    String waitId = delegateTask.getWaitId();
    waitNotifyEngine.notify(waitId, response.getResponse());
    wingsPersistence.delete(wingsPersistence.createQuery(DelegateTask.class)
                                .field("accountId")
                                .equal(response.getAccountId())
                                .field(ID_KEY)
                                .equal(response.getTaskId()));
  }

  @Override
  public File download(String managerHost, String accountId) throws IOException, TemplateException {
    File delegateFile = File.createTempFile("delegate", ".zip");
    File run = File.createTempFile("run", ".sh");
    File stop = File.createTempFile("stop", ".sh");

    ZipArchiveOutputStream out = new ZipArchiveOutputStream(delegateFile);
    out.putArchiveEntry(new ZipArchiveEntry("wings-delegate/"));
    out.closeArchiveEntry();
    Account account = accountService.get(accountId);
    try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(run))) {
      cfg.getTemplate("run.sh.ftl")
          .process(ImmutableMap.of("delegateMetadataUrl", mainConfiguration.getDelegateMetadataUrl(), "accountId",
                       accountId, "accountSecret", account.getAccountKey(), "managerHostAndPort", managerHost),
              fileWriter);
    }
    run = new File(run.getAbsolutePath());
    ZipArchiveEntry runZipArchiveEntry = new ZipArchiveEntry(run, "wings-delegate/run.sh");

    runZipArchiveEntry.setUnixMode(0755);
    AsiExtraField permissions = new AsiExtraField();
    permissions.setMode(0755);
    runZipArchiveEntry.addExtraField(permissions);

    out.putArchiveEntry(runZipArchiveEntry);
    try (FileInputStream fis = new FileInputStream(run)) {
      IOUtils.copy(fis, out);
    }
    out.closeArchiveEntry();

    try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(stop))) {
      cfg.getTemplate("stop.sh.ftl").process(null, fileWriter);
    }
    run = new File(run.getAbsolutePath());
    ZipArchiveEntry stopZipArchiveEntry = new ZipArchiveEntry(run, "wings-delegate/stop.sh");
    stopZipArchiveEntry.setUnixMode(0755);
    permissions = new AsiExtraField();
    permissions.setMode(0755);
    stopZipArchiveEntry.addExtraField(permissions);
    out.putArchiveEntry(stopZipArchiveEntry);
    try (FileInputStream fis = new FileInputStream(stop)) {
      IOUtils.copy(fis, out);
    }
    out.closeArchiveEntry();

    out.close();
    return delegateFile;
  }
}
