package software.wings.watcher.service;

import static com.google.common.collect.Iterables.isEmpty;
import static org.apache.commons.io.filefilter.FileFilterUtils.falseFileFilter;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static software.wings.utils.message.MessengerType.DELEGATE;
import static software.wings.utils.message.MessengerType.WATCHER;
import static software.wings.watcher.app.WatcherApplication.getProcessId;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Singleton;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import software.wings.utils.message.Message;
import software.wings.utils.message.MessageService;
import software.wings.watcher.app.WatcherApplication;
import software.wings.watcher.app.WatcherConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by brett on 10/26/17
 */
@Singleton
public class WatcherServiceImpl implements WatcherService {
  private static final long MAX_DELEGATE_HEARTBEAT_INTERVAL = TimeUnit.SECONDS.toMillis(15);
  private static final long MAX_DELEGATE_SHUTDOWN_GRACE_PERIOD = TimeUnit.HOURS.toMillis(2);
  private static final String DELEGATE_DASH = "delegate-";
  private static final String NEW_DELEGATE = "new-delegate";
  private static final String DELEGATE_STARTED = "delegate-started";
  private static final String STOP_ACQUIRING = "stop-acquiring";
  private static final String NEW_WATCHER = "new-watcher";
  private static final String WATCHER_STARTED = "watcher-started";
  private static final String GO_AHEAD = "go-ahead";

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Object waiter = new Object();
  private final List<Message> messageWaiter = new ArrayList<>(1);

  @Inject @Named("inputExecutor") private ScheduledExecutorService inputExecutor;
  @Inject @Named("watchExecutor") private ScheduledExecutorService watchExecutor;
  @Inject @Named("upgradeExecutor") private ScheduledExecutorService upgradeExecutor;
  @Inject private ExecutorService executorService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private Clock clock;
  @Inject private WatcherConfiguration watcherConfiguration;
  @Inject private MessageService messageService;
  @Inject private AmazonS3Client amazonS3Client;

  private boolean working;
  private List<String> runningDelegates;

  @Override
  public void run(boolean upgrade, boolean transition) {
    try {
      logger.info(upgrade ? "[New] Upgraded watcher process started" : "Watcher process started");
      runningDelegates = Optional.ofNullable((List) messageService.getData("watcher-data", "running-delegates"))
                             .orElse(new ArrayList<>());
      messageService.writeMessage(WATCHER_STARTED);
      startInputCheck();

      if (upgrade) {
        Message message = waitForIncomingMessage(GO_AHEAD, TimeUnit.MINUTES.toMillis(5));
        logger.info(message != null ? "[New] Got go-ahead. Proceeding"
                                    : "[New] Timed out waiting for go-ahead. Proceeding anyway");
      } else if (transition) {
        // TODO - Legacy path for transitioning from delegate only. Remove after watcher is standard
        logger.info("[New] Transitioned to watcher. Process started. Sending confirmation");
        System.out.println("botstarted"); // Don't remove this. It is used as message in upgrade flow.

        logger.info("[New] Waiting for go ahead from old delegate");
        int secs = 0;
        File goaheadFile = new File("goahead");
        while (!goaheadFile.exists() && secs++ < 7200) {
          Thread.sleep(1000L);
          logger.info("[New] Waiting for go ahead... ({} seconds elapsed)", secs);
        }

        if (secs < 7200) {
          logger.info("[New] Go ahead received from old delegate. Sending confirmation");
        } else {
          logger.info("[New] Timed out waiting for go ahead. Proceeding anyway");
        }
        System.out.println("proceeding"); // Don't remove this. It is used as message in upgrade flow.
      }

      startUpgradeCheck();
      startWatching();

      logger.info(upgrade ? "[New] Watcher upgraded" : "Watcher started");

      synchronized (waiter) {
        waiter.wait();
      }

    } catch (Exception e) {
      logger.error("Exception while running watcher", e);
    }
  }

  private void stop() {
    synchronized (waiter) {
      waiter.notify();
    }
  }

  private Message waitForIncomingMessage(String messageName, long timeout) {
    try {
      return timeLimiter.callWithTimeout(() -> {
        synchronized (messageWaiter) {
          messageWaiter.wait();
        }
        return messageWaiter.get(0);
      }, timeout, TimeUnit.MILLISECONDS, true);
    } catch (Exception e) {
      return null;
    }
  }

  private void startInputCheck() {
    inputExecutor.scheduleWithFixedDelay(() -> {
      Message message = messageService.readMessage(TimeUnit.MINUTES.toMillis(1));
      if (message != null) {
        switch (message.getMessage()) {
          case GO_AHEAD:
          case NEW_DELEGATE:
          case NEW_WATCHER:
            synchronized (messageWaiter) {
              messageWaiter.set(0, message);
              messageWaiter.notify();
            }
            break;
        }
      }
    }, 0, 1, TimeUnit.SECONDS);
  }

  private void startUpgradeCheck() {
    upgradeExecutor.scheduleWithFixedDelay(() -> {
      synchronized (this) {
        if (!working) {
          checkForUpgrade();
  }
}
}, 0, watcherConfiguration.getUpgradeCheckIntervalSeconds(), TimeUnit.SECONDS);
}

private void startWatching() {
    watchExecutor.scheduleWithFixedDelay(() -> {
      synchronized (this) {
        if (!working) {
          watchDelegate();
}
}
}, 0, 10, TimeUnit.SECONDS);
}

private void watchDelegate() {
  try {
    messageService.listDataNames(DELEGATE_DASH)
        .stream()
        .map(dataName -> dataName.substring(DELEGATE_DASH.length()))
        .filter(process -> !runningDelegates.contains(process))
        .forEach(process -> messageService.closeData(process));

    if (isEmpty(runningDelegates)) {
      working = true;
      startDelegate();
    } else {
      List<String> obsolete = new ArrayList<>();
      for (String delegateProcess : runningDelegates) {
        Map<String, Object> delegateData = messageService.getAllData(DELEGATE_DASH + delegateProcess);
        if (delegateData != null && !delegateData.isEmpty()) {
          long heartbeat = Optional.ofNullable((Long) delegateData.get("heartbeat")).orElse(0L);
          boolean restartNeeded = Optional.ofNullable((Boolean) delegateData.get("restartNeeded")).orElse(false);
          boolean upgradeNeeded = Optional.ofNullable((Boolean) delegateData.get("upgradeNeeded")).orElse(false);
          boolean shutdownPending = Optional.ofNullable((Boolean) delegateData.get("shutdownPending")).orElse(false);
          long shutdownStarted = Optional.ofNullable((Long) delegateData.get("shutdownStarted")).orElse(0L);

          if (shutdownPending) {
            working = true;
            if (clock.millis() - shutdownStarted > MAX_DELEGATE_SHUTDOWN_GRACE_PERIOD) {
              shutdownDelegate(delegateProcess);
            }
          } else if (clock.millis() - heartbeat > MAX_DELEGATE_HEARTBEAT_INTERVAL) {
            working = true;
            messageService.putData(DELEGATE_DASH + delegateProcess, "shutdownPending", true);
            messageService.putData(DELEGATE_DASH + delegateProcess, "shutdownStarted", clock.millis());
            restartDelegate(delegateProcess);
          } else if (restartNeeded) {
            working = true;
            restartDelegate(delegateProcess);
          } else if (upgradeNeeded) {
            working = true;
            upgradeDelegate(delegateProcess);
          }
        } else {
          obsolete.add(delegateProcess);
        }
      }
      runningDelegates.removeAll(obsolete);
      messageService.putData("watcher-data", "running-delegates", runningDelegates);
    }
  } catch (Exception e) {
    logger.error("Error processing delegate stream: {}", e.getMessage(), e);
  }
}

private void startDelegate() {
  startDelegateProcess(null, "DelegateStartScript", getProcessId());
}

private void restartDelegate(String oldDelegateProcess) {
  startDelegateProcess(oldDelegateProcess, "DelegateRestartScript", getProcessId());
}

private void upgradeDelegate(String oldDelegateProcess) {
  startDelegateProcess(oldDelegateProcess, "DelegateUpgradeScript", getProcessId());
}

private void startDelegateProcess(@Nullable String oldDelegateProcess, String scriptName, String watcherProcess) {
  executorService.submit(() -> {
    StartedProcess newDelegate = null;
    try {
      newDelegate = new ProcessExecutor()
                        .timeout(5, TimeUnit.MINUTES)
                        .command("nohup", "./delegate.sh", watcherProcess)
                        .redirectError(Slf4jStream.of(scriptName).asError())
                        .redirectOutput(Slf4jStream.of(scriptName).asInfo())
                        .readOutput(true)
                        .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
                        .start();

      if (newDelegate.getProcess().isAlive()) {
        Message message = waitForIncomingMessage(NEW_DELEGATE, TimeUnit.MINUTES.toMillis(2));
        if (message != null) {
          String newDelegateProcess = message.getParams().get(0);
          logger.info("Got process ID from new delegate: " + newDelegateProcess);
          message = messageService.retrieveMessage(DELEGATE, newDelegateProcess, TimeUnit.MINUTES.toMillis(2));
          if (message != null && message.getMessage().equals(DELEGATE_STARTED)) {
            if (oldDelegateProcess != null) {
              messageService.sendMessage(DELEGATE, oldDelegateProcess, STOP_ACQUIRING);
            }
            messageService.sendMessage(DELEGATE, newDelegateProcess, GO_AHEAD);
            runningDelegates.add(newDelegateProcess);
            messageService.putData("watcher-data", "running-delegates", runningDelegates);
          }
        }
      } else {
        newDelegate.getProcess().destroy();
        newDelegate.getProcess().waitFor();
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("[Old] Exception while upgrading", e);
      if (newDelegate != null) {
        try {
          newDelegate.getProcess().destroy();
          newDelegate.getProcess().waitFor();
        } catch (Exception ex) {
          // ignore
        }
        try {
          if (newDelegate.getProcess().isAlive()) {
            newDelegate.getProcess().destroyForcibly();
            if (newDelegate.getProcess() != null) {
              newDelegate.getProcess().waitFor();
            }
          }
        } catch (Exception ex) {
          logger.error("[Old] ALERT: Couldn't kill forcibly", ex);
        }
      }
    } finally {
      working = false;
    }
  });
}

private void shutdownDelegate(String delegateProcess) {
  executorService.submit(() -> {
    try {
      new ProcessExecutor().timeout(5, TimeUnit.SECONDS).command("kill", "-9", delegateProcess).start();
      messageService.closeData(DELEGATE_DASH + delegateProcess);
      messageService.closeChannel(DELEGATE, delegateProcess);
      runningDelegates.remove(delegateProcess);
      messageService.putData("watcher-data", "running-delegates", runningDelegates);
    } catch (Exception e) {
      logger.error("Error killing delegate {}", delegateProcess, e);
    } finally {
      working = false;
    }
  });
}

private void checkForUpgrade() {
  if (!watcherConfiguration.isDoUpgrade()) {
    logger.info("Auto upgrade is disabled in watcher configuration");
    logger.info("Watcher stays on version: [{}]", getVersion());
    return;
  }
  logger.info("Checking for upgrade");
  try {
    String watcherMetadataUrl = watcherConfiguration.getUpgradeCheckLocation();
    String bucketName =
        watcherMetadataUrl.substring(watcherMetadataUrl.indexOf("://") + 3, watcherMetadataUrl.indexOf(".s3"));
    String metaDataFileName = watcherMetadataUrl.substring(watcherMetadataUrl.lastIndexOf("/") + 1);
    S3Object obj = amazonS3Client.getObject(bucketName, metaDataFileName);
    BufferedReader reader = new BufferedReader(new InputStreamReader(obj.getObjectContent()));
    String watcherMetadata = reader.readLine();
    reader.close();
    String latestVersion = substringBefore(watcherMetadata, " ").trim();
    String watcherJarRelativePath = substringAfter(watcherMetadata, " ").trim();
    String version = getVersion();
    boolean upgrade = !StringUtils.equals(version, latestVersion);
    if (upgrade) {
      logger.info("[Old] Upgrading watcher");
      working = true;
      S3Object newVersionJarObj = amazonS3Client.getObject(bucketName, watcherJarRelativePath);
      upgradeWatcher(newVersionJarObj.getObjectContent(), getVersion(), latestVersion);
    } else {
      logger.info("Watcher up to date");
    }
  } catch (Exception e) {
    working = false;
    logger.error("[Old] Exception while checking for upgrade", e);
  }
}

private void upgradeWatcher(InputStream newVersionJarStream, String version, String newVersion)
    throws IOException, TimeoutException, InterruptedException {
  File watcherJarFile = new File("watcher.jar");
  FileUtils.copyInputStreamToFile(newVersionJarStream, watcherJarFile);

  StartedProcess process = null;
  try {
    logger.info("[Old] Upgrading the watcher");
    process = new ProcessExecutor()
                  .timeout(5, TimeUnit.MINUTES)
                  .command("nohup", "./start.sh", "upgrade", WatcherApplication.getProcessId())
                  .redirectError(Slf4jStream.of("UpgradeScript").asError())
                  .redirectOutput(Slf4jStream.of("UpgradeScript").asInfo())
                  .readOutput(true)
                  .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
                  .start();

    boolean success = false;

    if (process.getProcess().isAlive()) {
      Message message = waitForIncomingMessage(NEW_WATCHER, TimeUnit.MINUTES.toMillis(2));
      if (message != null) {
        String newWatcherProcessId = message.getParams().get(0);
        logger.info("[Old] Got process ID from new watcher: " + newWatcherProcessId);
        message = messageService.retrieveMessage(WATCHER, newWatcherProcessId, TimeUnit.MINUTES.toMillis(2));
        if (message != null && message.getMessage().equals(WATCHER_STARTED)) {
          messageService.sendMessage(WATCHER, newWatcherProcessId, GO_AHEAD);
          logger.info("[Old] Watcher upgraded. Stopping");
          removeWatcherVersionFromCapsule(version, newVersion);
          cleanupOldWatcherVersionFromBackup(version, newVersion);
          success = true;
          stop();
        }
      }
    }
    if (!success) {
      working = false;
      logger.error("[Old] Failed to upgrade watcher");
      process.getProcess().destroy();
      process.getProcess().waitFor();
    }
  } catch (Exception e) {
    e.printStackTrace();
    working = false;
    logger.error("[Old] Exception while upgrading", e);
    if (process != null) {
      try {
        process.getProcess().destroy();
        process.getProcess().waitFor();
      } catch (Exception ex) {
        // ignore
      }
      try {
        if (process.getProcess().isAlive()) {
          process.getProcess().destroyForcibly();
          if (process.getProcess() != null) {
            process.getProcess().waitFor();
          }
        }
      } catch (Exception ex) {
        logger.error("[Old] ALERT: Couldn't kill forcibly", ex);
      }
    }
  }
}

private void cleanupOldWatcherVersionFromBackup(String version, String newVersion) {
  try {
    cleanup(new File(System.getProperty("user.dir")), version, newVersion, "watcherBackup.");
  } catch (Exception ex) {
    logger.error(String.format("Failed to clean watcher version [%s] from Backup", newVersion), ex);
  }
}

private void removeWatcherVersionFromCapsule(String version, String newVersion) {
  try {
    cleanup(new File(System.getProperty("capsule.dir")).getParentFile(), version, newVersion, "watcher-");
  } catch (Exception ex) {
    logger.error(String.format("Failed to clean watcher version [%s] from Capsule", newVersion), ex);
  }
}

private void cleanup(File dir, String currentVersion, String newVersion, String pattern) {
  FileUtils.listFilesAndDirs(dir, falseFileFilter(), FileFilterUtils.prefixFileFilter(pattern)).forEach(file -> {
    if (!dir.equals(file) && !file.getName().contains(currentVersion) && !file.getName().contains(newVersion)) {
      logger.info("[Old] File Name to be deleted = " + file.getAbsolutePath());
      FileUtils.deleteQuietly(file);
    }
  });
}

private String getVersion() {
  return System.getProperty("version", "1.0.0-DEV");
}
}
