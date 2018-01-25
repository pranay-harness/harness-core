package software.wings.watcher.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedList;
import static org.apache.commons.io.filefilter.FileFilterUtils.falseFileFilter;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static software.wings.utils.message.MessageConstants.DELEGATE_DASH;
import static software.wings.utils.message.MessageConstants.DELEGATE_GO_AHEAD;
import static software.wings.utils.message.MessageConstants.DELEGATE_HEARTBEAT;
import static software.wings.utils.message.MessageConstants.DELEGATE_IS_NEW;
import static software.wings.utils.message.MessageConstants.DELEGATE_RESTART_NEEDED;
import static software.wings.utils.message.MessageConstants.DELEGATE_RESUME;
import static software.wings.utils.message.MessageConstants.DELEGATE_SHUTDOWN_PENDING;
import static software.wings.utils.message.MessageConstants.DELEGATE_SHUTDOWN_STARTED;
import static software.wings.utils.message.MessageConstants.DELEGATE_STARTED;
import static software.wings.utils.message.MessageConstants.DELEGATE_STOP_ACQUIRING;
import static software.wings.utils.message.MessageConstants.DELEGATE_UPGRADE_NEEDED;
import static software.wings.utils.message.MessageConstants.DELEGATE_UPGRADE_PENDING;
import static software.wings.utils.message.MessageConstants.DELEGATE_VERSION;
import static software.wings.utils.message.MessageConstants.EXTRA_WATCHER;
import static software.wings.utils.message.MessageConstants.NEW_DELEGATE;
import static software.wings.utils.message.MessageConstants.NEW_WATCHER;
import static software.wings.utils.message.MessageConstants.NEXT_WATCHER;
import static software.wings.utils.message.MessageConstants.RUNNING_DELEGATES;
import static software.wings.utils.message.MessageConstants.UPGRADING_DELEGATE;
import static software.wings.utils.message.MessageConstants.WATCHER_DATA;
import static software.wings.utils.message.MessageConstants.WATCHER_GO_AHEAD;
import static software.wings.utils.message.MessageConstants.WATCHER_HEARTBEAT;
import static software.wings.utils.message.MessageConstants.WATCHER_PROCESS;
import static software.wings.utils.message.MessageConstants.WATCHER_STARTED;
import static software.wings.utils.message.MessageConstants.WATCHER_VERSION;
import static software.wings.utils.message.MessengerType.DELEGATE;
import static software.wings.utils.message.MessengerType.WATCHER;
import static software.wings.watcher.app.WatcherApplication.getProcessId;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by brett on 10/26/17
 */
@Singleton
public class WatcherServiceImpl implements WatcherService {
  private static final Logger logger = LoggerFactory.getLogger(WatcherServiceImpl.class);

  private static final long DELEGATE_HEARTBEAT_TIMEOUT = TimeUnit.MINUTES.toMillis(3);
  private static final long DELEGATE_STARTUP_TIMEOUT = TimeUnit.MINUTES.toMillis(1);
  private static final long DELEGATE_SHUTDOWN_TIMEOUT = TimeUnit.HOURS.toMillis(2);

  @Inject @Named("inputExecutor") private ScheduledExecutorService inputExecutor;
  @Inject @Named("watchExecutor") private ScheduledExecutorService watchExecutor;
  @Inject @Named("upgradeExecutor") private ScheduledExecutorService upgradeExecutor;
  @Inject @Named("commandCheckExecutor") private ScheduledExecutorService commandCheckExecutor;
  @Inject private ExecutorService executorService;
  @Inject private Clock clock;
  @Inject private WatcherConfiguration watcherConfiguration;
  @Inject private MessageService messageService;
  @Inject private AmazonS3Client amazonS3Client;

  private final Object waiter = new Object();
  private final AtomicBoolean working = new AtomicBoolean(false);
  private final List<String> runningDelegates = synchronizedList(new ArrayList<>());

  private final AtomicInteger minMinorVersion = new AtomicInteger(0);

  @Override
  public void run(boolean upgrade) {
    try {
      logger.info(upgrade ? "[New] Upgraded watcher process started. Sending confirmation" : "Watcher process started");
      messageService.writeMessage(WATCHER_STARTED);
      startInputCheck();

      if (upgrade) {
        Message message = messageService.waitForMessage(WATCHER_GO_AHEAD, TimeUnit.MINUTES.toMillis(5));
        logger.info(message != null ? "[New] Got go-ahead. Proceeding"
                                    : "[New] Timed out waiting for go-ahead. Proceeding anyway");
      }

      messageService.removeData(WATCHER_DATA, NEXT_WATCHER);
      startUpgradeCheck();
      startCommandCheck();
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

  private void startInputCheck() {
    inputExecutor.scheduleWithFixedDelay(
        messageService.getMessageCheckingRunnable(TimeUnit.SECONDS.toMillis(4), null), 0, 1, TimeUnit.SECONDS);
  }

  private void startUpgradeCheck() {
    upgradeExecutor.scheduleWithFixedDelay(() -> {
      boolean forCodeFormattingOnly; // This line is here for clang-format
      synchronized (this) {
        if (!working.get()) {
          checkForWatcherUpgrade();
        }
      }
    }, 0, watcherConfiguration.getUpgradeCheckIntervalSeconds(), TimeUnit.SECONDS);
  }

  private void startCommandCheck() {
    commandCheckExecutor.scheduleWithFixedDelay(() -> {
      boolean forCodeFormattingOnly; // This line is here for clang-format
      synchronized (this) {
        checkForCommands();
      }
    }, 0, 3, TimeUnit.MINUTES);
  }

  @SuppressWarnings({"unchecked"})
  private void startWatching() {
    runningDelegates.addAll(
        Optional.ofNullable(messageService.getData(WATCHER_DATA, RUNNING_DELEGATES, List.class)).orElse(emptyList()));

    watchExecutor.scheduleWithFixedDelay(() -> {
      Map<String, Object> heartbeatData = new HashMap<>();
      heartbeatData.put(WATCHER_HEARTBEAT, clock.millis());
      heartbeatData.put(WATCHER_PROCESS, getProcessId());
      heartbeatData.put(WATCHER_VERSION, getVersion());
      messageService.putAllData(WATCHER_DATA, heartbeatData);
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
            if (!Optional.ofNullable(messageService.getData(DELEGATE_DASH + process, DELEGATE_IS_NEW, Boolean.class))
                     .orElse(false)) {
              logger.warn("Data found for untracked delegate process {}. Shutting it down", process);
              shutdownDelegate(process);
            }
          });

      messageService.listChannels(DELEGATE)
          .stream()
          .filter(process -> !runningDelegates.contains(process))
          .forEach(process -> {
            logger.warn("Message channel found for untracked delegate process {}. Shutting it down", process);
            shutdownDelegate(process);
          });

      String extraWatcher = messageService.getData(WATCHER_DATA, EXTRA_WATCHER, String.class);
      if (isNotEmpty(extraWatcher)) {
        if (!StringUtils.equals(extraWatcher, getProcessId())) {
          logger.warn("Shutting down extra watcher {}", extraWatcher);
          executorService.submit(() -> {
            try {
              new ProcessExecutor().timeout(5, TimeUnit.SECONDS).command("kill", "-9", extraWatcher).start();
            } catch (Exception e) {
              logger.error("Error killing watcher {}", extraWatcher, e);
            }
            messageService.closeChannel(WATCHER, extraWatcher);
            messageService.removeData(WATCHER_DATA, EXTRA_WATCHER);
          });
        }
      } else {
        messageService.listChannels(WATCHER)
            .stream()
            .filter(process
                -> !StringUtils.equals(process, getProcessId())
                    && !StringUtils.equals(process, messageService.getData(WATCHER_DATA, NEXT_WATCHER, String.class)))
            .forEach(process -> {
              logger.warn(
                  "Message channel found for another watcher process that isn't the next watcher. {} will be shut down",
                  process);
              messageService.putData(WATCHER_DATA, EXTRA_WATCHER, process);
            });
      }

      if (isEmpty(runningDelegates)) {
        if (working.compareAndSet(false, true)) {
          startDelegateProcess(emptyList(), "DelegateStartScript", getProcessId());
        }
      } else {
        List<String> obsolete = new ArrayList<>();
        List<String> restartNeededList = new ArrayList<>();
        List<String> upgradeNeededList = new ArrayList<>();
        List<String> shutdownNeededList = new ArrayList<>();
        List<String> shutdownPendingList = new ArrayList<>();
        String upgradePendingDelegate = null;
        boolean newDelegateTimedOut = false;
        long now = clock.millis();

        synchronized (runningDelegates) {
          for (String delegateProcess : runningDelegates) {
            Map<String, Object> delegateData = messageService.getAllData(DELEGATE_DASH + delegateProcess);
            if (MapUtils.isNotEmpty(delegateData)) {
              String delegateVersion = (String) delegateData.get(DELEGATE_VERSION);
              Integer delegateMinorVersion = getMinorVersion(delegateVersion);

              long heartbeat = Optional.ofNullable((Long) delegateData.get(DELEGATE_HEARTBEAT)).orElse(0L);
              boolean newDelegate = Optional.ofNullable((Boolean) delegateData.get(DELEGATE_IS_NEW)).orElse(false);
              boolean restartNeeded =
                  Optional.ofNullable((Boolean) delegateData.get(DELEGATE_RESTART_NEEDED)).orElse(false);
              boolean upgradeNeeded =
                  Optional.ofNullable((Boolean) delegateData.get(DELEGATE_UPGRADE_NEEDED)).orElse(false);
              boolean upgradePending =
                  Optional.ofNullable((Boolean) delegateData.get(DELEGATE_UPGRADE_PENDING)).orElse(false);
              boolean shutdownPending =
                  Optional.ofNullable((Boolean) delegateData.get(DELEGATE_SHUTDOWN_PENDING)).orElse(false);
              long shutdownStarted = Optional.ofNullable((Long) delegateData.get(DELEGATE_SHUTDOWN_STARTED)).orElse(0L);

              if (newDelegate) {
                logger.info("New delegate process {} is starting", delegateProcess);
                if (now - heartbeat > DELEGATE_STARTUP_TIMEOUT) {
                  newDelegateTimedOut = true;
                  shutdownNeededList.add(delegateProcess);
                }
              } else if (shutdownPending) {
                logger.info(
                    "Shutdown is pending for delegate process {} with version {}", delegateProcess, delegateVersion);
                shutdownPendingList.add(delegateProcess);
                if (now - shutdownStarted > DELEGATE_SHUTDOWN_TIMEOUT || now - heartbeat > DELEGATE_HEARTBEAT_TIMEOUT) {
                  shutdownNeededList.add(delegateProcess);
                }
              } else if (restartNeeded || now - heartbeat > DELEGATE_HEARTBEAT_TIMEOUT
                  || (delegateMinorVersion != null && delegateMinorVersion < minMinorVersion.get())) {
                restartNeededList.add(delegateProcess);
                minMinorVersion.set(0);
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

          if (isNotEmpty(obsolete)) {
            logger.info("Obsolete processes {} no longer tracked", obsolete);
            runningDelegates.removeAll(obsolete);
            messageService.putData(WATCHER_DATA, RUNNING_DELEGATES, runningDelegates);
            obsolete.forEach(this ::shutdownDelegate);
          }

          if (shutdownPendingList.containsAll(runningDelegates)) {
            logger.warn("No delegates acquiring tasks. Delegate processes {} pending shutdown. Forcing shutdown now",
                shutdownPendingList);
            shutdownPendingList.forEach(this ::shutdownDelegate);
          }
        }

        if (isNotEmpty(shutdownNeededList)) {
          logger.warn("Delegate processes {} exceeded grace period. Forcing shutdown", shutdownNeededList);
          shutdownNeededList.forEach(this ::shutdownDelegate);
          if (newDelegateTimedOut && upgradePendingDelegate != null) {
            logger.warn("New delegate failed to start. Resuming old delegate {}", upgradePendingDelegate);
            messageService.writeMessageToChannel(DELEGATE, upgradePendingDelegate, DELEGATE_RESUME);
          }
        }
        if (isNotEmpty(restartNeededList)) {
          logger.warn("Delegate processes {} need restart. Shutting down", restartNeededList);
          restartNeededList.forEach(this ::shutdownDelegate);
        }
        if (isNotEmpty(upgradeNeededList)) {
          if (working.compareAndSet(false, true)) {
            logger.info("Delegate processes {} ready for upgrade. Sending confirmation", upgradeNeededList);
            upgradeNeededList.forEach(
                delegateProcess -> messageService.writeMessageToChannel(DELEGATE, delegateProcess, UPGRADING_DELEGATE));
            startDelegateProcess(upgradeNeededList, "DelegateUpgradeScript", getProcessId());
          }
        }
      }
    } catch (Exception e) {
      logger.error("Error processing delegate stream: {}", e.getMessage(), e);
    }
  }

  private Integer getMinorVersion(String delegateVersion) {
    Integer delegateVersionNumber = null;
    if (isNotBlank(delegateVersion)) {
      try {
        delegateVersionNumber = Integer.parseInt(delegateVersion.substring(delegateVersion.lastIndexOf('.') + 1));
      } catch (NumberFormatException e) {
        delegateVersionNumber = null;
      }
    }
    return delegateVersionNumber;
  }

  private void startDelegateProcess(List<String> oldDelegateProcesses, String scriptName, String watcherProcess) {
    if (!new File("delegate.sh").exists()) {
      working.set(false);
      return;
    }

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

        boolean success = false;

        if (newDelegate.getProcess().isAlive()) {
          Message message = messageService.waitForMessage(NEW_DELEGATE, TimeUnit.MINUTES.toMillis(2));
          if (message != null) {
            String newDelegateProcess = message.getParams().get(0);
            logger.info("Got process ID from new delegate: " + newDelegateProcess);
            Map<String, Object> delegateData = new HashMap<>();
            delegateData.put(DELEGATE_IS_NEW, true);
            delegateData.put(DELEGATE_HEARTBEAT, clock.millis());
            messageService.putAllData(DELEGATE_DASH + newDelegateProcess, delegateData);
            synchronized (runningDelegates) {
              runningDelegates.add(newDelegateProcess);
              messageService.putData(WATCHER_DATA, RUNNING_DELEGATES, runningDelegates);
            }
            message = messageService.readMessageFromChannel(DELEGATE, newDelegateProcess, TimeUnit.MINUTES.toMillis(2));
            if (message != null && message.getMessage().equals(DELEGATE_STARTED)) {
              logger.info("Retrieved delegate-started message from new delegate {}", newDelegateProcess);
              oldDelegateProcesses.forEach(oldDelegateProcess -> {
                logger.info("Sending old delegate process {} stop-acquiring message", oldDelegateProcess);
                messageService.writeMessageToChannel(DELEGATE, oldDelegateProcess, DELEGATE_STOP_ACQUIRING);
              });
              logger.info("Sending new delegate process {} go-ahead message", newDelegateProcess);
              messageService.writeMessageToChannel(DELEGATE, newDelegateProcess, DELEGATE_GO_AHEAD);
              success = true;
            }
          }
        }
        if (!success) {
          logger.error("Failed to start new delegate");
          newDelegate.getProcess().destroy();
          newDelegate.getProcess().waitFor();
          oldDelegateProcesses.forEach(oldDelegateProcess -> {
            logger.info("Sending old delegate process {} resume message", oldDelegateProcess);
            messageService.writeMessageToChannel(DELEGATE, oldDelegateProcess, DELEGATE_RESUME);
          });
        }
      } catch (Exception e) {
        e.printStackTrace();
        logger.error("Exception while upgrading", e);
        oldDelegateProcesses.forEach(oldDelegateProcess -> {
          logger.info("Sending old delegate process {} resume message", oldDelegateProcess);
          messageService.writeMessageToChannel(DELEGATE, oldDelegateProcess, DELEGATE_RESUME);
        });
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
            logger.error("ALERT: Couldn't kill forcibly", ex);
          }
        }
      } finally {
        working.set(false);
      }
    });
  }

  private void shutdownDelegate(String delegateProcess) {
    executorService.submit(() -> {
      messageService.writeMessageToChannel(DELEGATE, delegateProcess, DELEGATE_STOP_ACQUIRING);
      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        new ProcessExecutor().timeout(5, TimeUnit.SECONDS).command("kill", "-9", delegateProcess).start();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        logger.error("Error killing delegate {}", delegateProcess, e);
      }
      messageService.closeData(DELEGATE_DASH + delegateProcess);
      messageService.closeChannel(DELEGATE, delegateProcess);
      synchronized (runningDelegates) {
        runningDelegates.remove(delegateProcess);
        messageService.putData(WATCHER_DATA, RUNNING_DELEGATES, runningDelegates);
      }
    });
  }

  private void checkForWatcherUpgrade() {
    if (!watcherConfiguration.isDoUpgrade()) {
      logger.info("Auto upgrade is disabled in watcher configuration");
      logger.info("Watcher stays on version: [{}]", getVersion());
      return;
    }
    try {
      String watcherMetadataUrl = watcherConfiguration.getUpgradeCheckLocation();
      String bucketName =
          watcherMetadataUrl.substring(watcherMetadataUrl.indexOf("://") + 3, watcherMetadataUrl.indexOf(".s3"));
      String metaDataFileName = watcherMetadataUrl.substring(watcherMetadataUrl.lastIndexOf('/') + 1);
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
        upgradeWatcher(bucketName, watcherJarRelativePath, getVersion(), latestVersion);
      } else {
        logger.info("Watcher up to date");
      }
    } catch (Exception e) {
      working.set(false);
      logger.error("Exception while checking for upgrade", e);
    }
  }

  private void checkForCommands() {
    try {
      String watcherMetadataUrl = watcherConfiguration.getUpgradeCheckLocation();
      String bucketName =
          watcherMetadataUrl.substring(watcherMetadataUrl.indexOf("://") + 3, watcherMetadataUrl.indexOf(".s3"));
      String env = watcherMetadataUrl.substring(watcherMetadataUrl.lastIndexOf('/') + 8);
      S3Object commandsObj = amazonS3Client.getObject(bucketName, "commands/" + env);
      if (commandsObj != null) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(commandsObj.getObjectContent()));
        String line;
        while ((line = reader.readLine()) != null) {
          String cmd = substringBefore(line, " ").trim();
          String param = substringAfter(line, " ").trim();

          if ("minVersion".equals(cmd)) {
            int minVersion = Integer.parseInt(param);
            if (minMinorVersion.getAndSet(minVersion) != minVersion) {
              logger.info("Setting minimum delegate version: {}", minVersion);
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error("Exception while checking for commands", e);
    }
  }

  private void upgradeWatcher(String bucketName, String watcherJarRelativePath, String version, String newVersion)
      throws IOException {
    S3Object newVersionJarObj = amazonS3Client.getObject(bucketName, watcherJarRelativePath);
    InputStream newVersionJarStream = newVersionJarObj.getObjectContent();
    File watcherJarFile = new File("watcher.jar");
    FileUtils.copyInputStreamToFile(newVersionJarStream, watcherJarFile);
    updateStartScript(newVersion, watcherJarRelativePath);

    StartedProcess process = null;
    try {
      logger.info("[Old] Starting new watcher");
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
        Message message = messageService.waitForMessage(NEW_WATCHER, TimeUnit.MINUTES.toMillis(2));
        if (message != null) {
          String newWatcherProcess = message.getParams().get(0);
          logger.info("[Old] Got process ID from new watcher: " + newWatcherProcess);
          messageService.putData(WATCHER_DATA, NEXT_WATCHER, newWatcherProcess);
          message = messageService.readMessageFromChannel(WATCHER, newWatcherProcess, TimeUnit.MINUTES.toMillis(2));
          if (message != null && message.getMessage().equals(WATCHER_STARTED)) {
            logger.info(
                "[Old] Retrieved watcher-started message from new watcher {}. Sending go-ahead", newWatcherProcess);
            messageService.writeMessageToChannel(WATCHER, newWatcherProcess, WATCHER_GO_AHEAD);
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

  private void updateStartScript(String newVersion, String watcherJarRelativePath) {
    String remoteWatcherVersionPrefix = "REMOTE_WATCHER_VERSION=";
    String remoteWatcherUrlPrefix = "REMOTE_WATCHER_URL=http://wingswatchers.s3-website-us-east-1.amazonaws.com/";
    try {
      File start = new File("start.sh");
      List<String> outLines = new ArrayList<>();
      for (String line : FileUtils.readLines(start, UTF_8)) {
        if (StringUtils.startsWith(line, remoteWatcherVersionPrefix)) {
          outLines.add(remoteWatcherVersionPrefix + newVersion);
        } else if (StringUtils.startsWith(line, remoteWatcherUrlPrefix)) {
          outLines.add(remoteWatcherUrlPrefix + watcherJarRelativePath);
        } else {
          outLines.add(line);
        }
      }
      FileUtils.forceDelete(start);
      FileUtils.touch(start);
      FileUtils.writeLines(start, outLines);
      Files.setPosixFilePermissions(start.toPath(),
          Sets.newHashSet(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE,
              PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ));
    } catch (Exception e) {
      logger.error("Error modifying start script.", e);
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
