package software.wings.watcher.service;

import static com.google.common.collect.Iterables.isEmpty;
import static com.hazelcast.util.CollectionUtil.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedList;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by brett on 10/26/17
 */
@Singleton
public class WatcherServiceImpl implements WatcherService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final long MAX_DELEGATE_HEARTBEAT_INTERVAL = TimeUnit.SECONDS.toMillis(30);
  private static final long MAX_DELEGATE_STARTUP_GRACE_PERIOD = TimeUnit.MINUTES.toMillis(2);
  private static final long MAX_DELEGATE_SHUTDOWN_GRACE_PERIOD = TimeUnit.HOURS.toMillis(2);

  private static final String DELEGATE_DASH = "delegate-";
  private static final String NEW_DELEGATE = "new-delegate";
  private static final String DELEGATE_STARTED = "delegate-started";
  private static final String UPGRADING = "upgrading";
  private static final String STOP_ACQUIRING = "stop-acquiring";
  private static final String NEW_WATCHER = "new-watcher";
  private static final String WATCHER_STARTED = "watcher-started";
  private static final String GO_AHEAD = "go-ahead";
  private static final String RESUME = "resume";
  private static final String WATCHER_DATA = "watcher-data";
  private static final String RUNNING_DELEGATES = "running-delegates";
  private static final String NEXT_WATCHER = "next-watcher";

  @Inject @Named("inputExecutor") private ScheduledExecutorService inputExecutor;
  @Inject @Named("watchExecutor") private ScheduledExecutorService watchExecutor;
  @Inject @Named("upgradeExecutor") private ScheduledExecutorService upgradeExecutor;
  @Inject private ExecutorService executorService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private Clock clock;
  @Inject private WatcherConfiguration watcherConfiguration;
  @Inject private MessageService messageService;
  @Inject private AmazonS3Client amazonS3Client;

  private final Object waiter = new Object();
  private final AtomicBoolean working = new AtomicBoolean(false);
  private final List<String> runningDelegates = synchronizedList(new ArrayList<>());
  private final BlockingQueue<Message> watcherMessages = new LinkedBlockingQueue<>();

  @Override
  @SuppressWarnings({"unchecked"})
  public void run(boolean upgrade, boolean transition) {
    try {
      logger.info(upgrade ? "[New] Upgraded watcher process started" : "Watcher process started");
      runningDelegates.addAll(
          Optional.ofNullable(messageService.getData(WATCHER_DATA, RUNNING_DELEGATES, List.class)).orElse(emptyList()));
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

      messageService.removeData(WATCHER_DATA, NEXT_WATCHER);
      startUpgradeCheck();
      startWatching();

      logger.info(upgrade ? "[New] Watcher upgraded" : "Watcher started");

      synchronized (waiter) {
        waiter.wait();
      }

      messageService.closeChannel(WATCHER, getProcessId());

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
        Message message = null;
        while (message == null || !messageName.equals(message.getMessage())) {
          try {
            message = watcherMessages.take();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
        return message;
      }, timeout, TimeUnit.MILLISECONDS, true);
    } catch (Exception e) {
      return null;
    }
  }

  private void startInputCheck() {
    inputExecutor.scheduleWithFixedDelay(() -> {
      Message message = messageService.readMessage(TimeUnit.SECONDS.toMillis(4));
      if (message != null) {
        try {
          watcherMessages.put(message);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }, 0, 1, TimeUnit.SECONDS);
  }

  private void startUpgradeCheck() {
    upgradeExecutor.scheduleWithFixedDelay(() -> {
      synchronized (this) {
        if (!working.get()) {
          checkForUpgrade();
  }
}
}, 0, watcherConfiguration.getUpgradeCheckIntervalSeconds(), TimeUnit.SECONDS);
}

private void startWatching() {
    watchExecutor.scheduleWithFixedDelay(() -> {
      synchronized (this) {
        if (!working.get()) {
          watchDelegate();
}
}
}, 0, 10, TimeUnit.SECONDS);
}

private void watchDelegate() {
  try {
    logger.info("Watching delegate processes: {}", runningDelegates);
    // Cleanup obsolete
    messageService.listDataNames(DELEGATE_DASH)
        .stream()
        .map(dataName -> dataName.substring(DELEGATE_DASH.length()))
        .filter(process -> !runningDelegates.contains(process))
        .forEach(process -> {
          if (!Optional.ofNullable(messageService.getData(DELEGATE_DASH + process, "newDelegate", Boolean.class))
                   .orElse(false)) {
            logger.info("Data found for untracked delegate process {}. Shutting it down", process);
            shutdownDelegate(process);
          }
        });

    messageService.listChannels(DELEGATE)
        .stream()
        .filter(process -> !runningDelegates.contains(process))
        .forEach(process -> {
          logger.info("Message channel found for untracked delegate process {}. Shutting it down", process);
          shutdownDelegate(process);
        });

    messageService.listChannels(WATCHER)
        .stream()
        .filter(process
            -> !StringUtils.equals(process, getProcessId())
                && !StringUtils.equals(process, messageService.getData(WATCHER_DATA, NEXT_WATCHER, String.class)))
        .forEach(process -> {
          logger.info(
              "Message channel found for another watcher process {} that isn't the next watcher. Closing channel",
              process);
          messageService.closeChannel(WATCHER, process);
        });

    if (isEmpty(runningDelegates)) {
      if (working.compareAndSet(false, true)) {
        startDelegateProcess(emptyList(), "DelegateStartScript", getProcessId());
      }
    } else {
      List<String> obsolete = new ArrayList<>();
      List<String> restartNeededList = new ArrayList<>();
      List<String> upgradeNeededList = new ArrayList<>();
      List<String> shutdownNeededList = new ArrayList<>();
      String upgradePendingDelegate = null;
      boolean newDelegateTimedOut = false;
      synchronized (runningDelegates) {
        for (String delegateProcess : runningDelegates) {
          Map<String, Object> delegateData = messageService.getAllData(DELEGATE_DASH + delegateProcess);
          if (delegateData != null && !delegateData.isEmpty()) {
            long heartbeat = Optional.ofNullable((Long) delegateData.get("heartbeat")).orElse(0L);
            boolean newDelegate = Optional.ofNullable((Boolean) delegateData.get("newDelegate")).orElse(false);
            boolean restartNeeded = Optional.ofNullable((Boolean) delegateData.get("restartNeeded")).orElse(false);
            boolean upgradeNeeded = Optional.ofNullable((Boolean) delegateData.get("upgradeNeeded")).orElse(false);
            boolean upgradePending = Optional.ofNullable((Boolean) delegateData.get("upgradePending")).orElse(false);
            boolean shutdownPending = Optional.ofNullable((Boolean) delegateData.get("shutdownPending")).orElse(false);
            long shutdownStarted = Optional.ofNullable((Long) delegateData.get("shutdownStarted")).orElse(0L);

            if (newDelegate) {
              logger.info("New delegate process {} is starting", delegateProcess);
              if (clock.millis() - heartbeat > MAX_DELEGATE_STARTUP_GRACE_PERIOD) {
                newDelegateTimedOut = true;
                shutdownNeededList.add(delegateProcess);
              }
            } else if (shutdownPending) {
              logger.info("Shutdown is pending for {}", delegateProcess);
              if (clock.millis() - shutdownStarted > MAX_DELEGATE_SHUTDOWN_GRACE_PERIOD) {
                shutdownNeededList.add(delegateProcess);
              }
            } else if (restartNeeded || clock.millis() - heartbeat > MAX_DELEGATE_HEARTBEAT_INTERVAL) {
              restartNeededList.add(delegateProcess);
            } else if (upgradeNeeded) {
              upgradeNeededList.add(delegateProcess);
            }
            if (upgradePending) {
              upgradePendingDelegate = delegateProcess;
            }
          } else {
            obsolete.add(delegateProcess);
          }
        }
      }

      if (isNotEmpty(shutdownNeededList)) {
        logger.warn("Delegate processes {} exceeded grace period. Forcing shutdown", shutdownNeededList);
        shutdownNeededList.forEach(this ::shutdownDelegate);
        if (newDelegateTimedOut && upgradePendingDelegate != null) {
          logger.info("New delegate failed to start. Resuming old delegate {}", upgradePendingDelegate);
          messageService.sendMessage(DELEGATE, upgradePendingDelegate, RESUME);
        }
      }
      if (isNotEmpty(restartNeededList)) {
        logger.warn("Delegate processes {} need restart. Shutting down", restartNeededList);
        restartNeededList.forEach(this ::shutdownDelegate);
      }
      if (isNotEmpty(upgradeNeededList)) {
        if (working.compareAndSet(false, true)) {
          logger.info("Delegate processes {} ready for upgrade", upgradeNeededList);
          upgradeNeededList.forEach(
              delegateProcess -> messageService.sendMessage(DELEGATE, delegateProcess, UPGRADING));
          startDelegateProcess(upgradeNeededList, "DelegateUpgradeScript", getProcessId());
        }
      }

      if (isNotEmpty(obsolete)) {
        logger.info("Obsolete processes {} no longer tracked", obsolete);
        synchronized (runningDelegates) {
          runningDelegates.removeAll(obsolete);
          messageService.putData(WATCHER_DATA, RUNNING_DELEGATES, runningDelegates);
        }
      }
    }
  } catch (Exception e) {
    logger.error("Error processing delegate stream: {}", e.getMessage(), e);
  }
}

private void startDelegateProcess(List<String> oldDelegateProcesses, String scriptName, String watcherProcess) {
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
          messageService.putData(DELEGATE_DASH + newDelegateProcess, "newDelegate", true);
          messageService.putData(DELEGATE_DASH + newDelegateProcess, "heartbeat", clock.millis());
          synchronized (runningDelegates) {
            runningDelegates.add(newDelegateProcess);
            messageService.putData(WATCHER_DATA, RUNNING_DELEGATES, runningDelegates);
          }
          message = messageService.retrieveMessage(DELEGATE, newDelegateProcess, TimeUnit.MINUTES.toMillis(2));
          if (message != null && message.getMessage().equals(DELEGATE_STARTED)) {
            oldDelegateProcesses.forEach(
                oldDelegateProcess -> messageService.sendMessage(DELEGATE, oldDelegateProcess, STOP_ACQUIRING));
            messageService.sendMessage(DELEGATE, newDelegateProcess, GO_AHEAD);
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
      working.set(false);
    }
  });
}

private void shutdownDelegate(String delegateProcess) {
  executorService.submit(() -> {
    try {
      new ProcessExecutor().timeout(5, TimeUnit.SECONDS).command("kill", "-9", delegateProcess).start();
      messageService.closeData(DELEGATE_DASH + delegateProcess);
      messageService.closeChannel(DELEGATE, delegateProcess);
      synchronized (runningDelegates) {
        runningDelegates.remove(delegateProcess);
        messageService.putData(WATCHER_DATA, RUNNING_DELEGATES, runningDelegates);
      }
    } catch (Exception e) {
      logger.error("Error killing delegate {}", delegateProcess, e);
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
      working.set(true);
      S3Object newVersionJarObj = amazonS3Client.getObject(bucketName, watcherJarRelativePath);
      upgradeWatcher(newVersionJarObj.getObjectContent(), getVersion(), latestVersion);
    } else {
      logger.info("Watcher up to date");
    }
  } catch (Exception e) {
    working.set(false);
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
        messageService.putData(WATCHER_DATA, NEXT_WATCHER, newWatcherProcessId);
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
      logger.error("[Old] Failed to upgrade watcher");
      process.getProcess().destroy();
      process.getProcess().waitFor();
    }
  } catch (Exception e) {
    e.printStackTrace();
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
  } finally {
    working.set(false);
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
