package software.wings.watcher.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.synchronizedList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.filefilter.FileFilterUtils.falseFileFilter;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static software.wings.managerclient.SafeHttpCall.execute;
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
import static software.wings.utils.message.MessageConstants.DELEGATE_UPGRADE_STARTED;
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
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.network.Http;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import software.wings.beans.DelegateConfiguration;
import software.wings.beans.DelegateScripts;
import software.wings.beans.RestResponse;
import software.wings.managerclient.ManagerClient;
import software.wings.utils.message.Message;
import software.wings.utils.message.MessageService;
import software.wings.watcher.app.WatcherApplication;
import software.wings.watcher.app.WatcherConfiguration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Created by brett on 10/26/17
 */
@Singleton
public class WatcherServiceImpl implements WatcherService {
  private static final Logger logger = LoggerFactory.getLogger(WatcherServiceImpl.class);

  private static final long DELEGATE_HEARTBEAT_TIMEOUT = TimeUnit.MINUTES.toMillis(3);
  private static final long DELEGATE_STARTUP_TIMEOUT = TimeUnit.MINUTES.toMillis(1);
  private static final long DELEGATE_UPGRADE_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
  private static final long DELEGATE_SHUTDOWN_TIMEOUT = TimeUnit.HOURS.toMillis(2);
  private static final long DELEGATE_VERSION_MATCH_TIMEOUT = TimeUnit.HOURS.toMillis(2);
  private static final String NEW_DELEGATE_VERSION = "new-delegate-version";

  @Inject @Named("inputExecutor") private ScheduledExecutorService inputExecutor;
  @Inject @Named("watchExecutor") private ScheduledExecutorService watchExecutor;
  @Inject @Named("upgradeExecutor") private ScheduledExecutorService upgradeExecutor;
  @Inject @Named("commandCheckExecutor") private ScheduledExecutorService commandCheckExecutor;
  @Inject private ExecutorService executorService;
  @Inject private Clock clock;
  @Inject private TimeLimiter timeLimiter;
  @Inject private WatcherConfiguration watcherConfiguration;
  @Inject private MessageService messageService;
  @Inject private ManagerClient managerClient;

  private final Object waiter = new Object();
  private final AtomicBoolean working = new AtomicBoolean(false);
  private final List<String> runningDelegates = synchronizedList(new ArrayList<>());

  private final AtomicInteger minMinorVersion = new AtomicInteger(0);
  private final Set<Integer> illegalVersions = new HashSet<>();
  private final Map<String, Long> delegateVersionMatchedAt = new HashMap<>();
  private HttpHost httpProxyHost;

  private final boolean multiVersion = TRUE.toString().equals(System.getenv().get("MULTI_VERSION"));

  @SuppressFBWarnings({"UW_UNCOND_WAIT", "WA_NOT_IN_LOOP"})
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

      logger.info(upgrade ? "[New] Watcher upgraded" : "Watcher started");

      String proxyHost = System.getProperty("https.proxyHost");

      if (isNotBlank(proxyHost)) {
        String proxyScheme = System.getProperty("proxyScheme");
        String proxyPort = System.getProperty("https.proxyPort");
        logger.info("Using {} proxy {}:{}", proxyScheme, proxyHost, proxyPort);
        httpProxyHost = new HttpHost(proxyHost, Integer.parseInt(proxyPort), proxyScheme);
        String nonProxyHostsString = System.getProperty("http.nonProxyHosts");
        if (isNotBlank(nonProxyHostsString)) {
          String[] suffixes = nonProxyHostsString.split("\\|");
          List<String> nonProxyHosts = Stream.of(suffixes).map(suffix -> suffix.substring(1)).collect(toList());
          logger.info("No proxy for hosts with suffix in: {}", nonProxyHosts);
        }
      } else {
        logger.info("No proxy settings. Configure in proxy.config if needed");
      }

      startUpgradeCheck();
      startCommandCheck();
      startWatching();

      synchronized (waiter) {
        waiter.wait();
      }

      messageService.closeChannel(WATCHER, getProcessId());

    } catch (InterruptedException e) {
      logger.error("Interrupted while running watcher", e);
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
        shutdownWatcher(extraWatcher);
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

      List<String> expectedVersions = findExpectedDelegateVersions();
      List<String> runningVersions = new ArrayList<>();

      if (isEmpty(runningDelegates)) {
        if (!multiVersion) {
          if (working.compareAndSet(false, true)) {
            startDelegateProcess(".", emptyList(), "DelegateStartScript", getProcessId());
          }
        }
      } else {
        List<String> obsolete = new ArrayList<>();
        List<String> restartNeededList = new ArrayList<>();
        List<String> upgradeNeededList = new ArrayList<>();
        List<String> shutdownNeededList = new ArrayList<>();
        List<String> shutdownPendingList = new ArrayList<>();
        List<String> drainingNeededList = new ArrayList<>();
        String upgradePendingDelegate = null;
        boolean newDelegateTimedOut = false;
        long now = clock.millis();

        synchronized (runningDelegates) {
          Set<String> notRunning = delegateVersionMatchedAt.keySet()
                                       .stream()
                                       .filter(delegateProcess -> !runningDelegates.contains(delegateProcess))
                                       .collect(toSet());
          notRunning.forEach(delegateVersionMatchedAt::remove);

          for (String delegateProcess : runningDelegates) {
            Map<String, Object> delegateData = messageService.getAllData(DELEGATE_DASH + delegateProcess);
            if (isNotEmpty(delegateData)) {
              String delegateVersion = (String) delegateData.get(DELEGATE_VERSION);
              runningVersions.add(delegateVersion);
              Integer delegateMinorVersion = getMinorVersion(delegateVersion);
              boolean delegateMinorVersionMismatch = delegateMinorVersion != null
                  && (delegateMinorVersion < minMinorVersion.get() || illegalVersions.contains(delegateMinorVersion));
              if (!delegateVersionMatchedAt.containsKey(delegateProcess)
                  || expectedVersions.contains(delegateVersion)) {
                delegateVersionMatchedAt.put(delegateProcess, now);
              }
              boolean versionMatchTimedOut =
                  now - delegateVersionMatchedAt.get(delegateProcess) > DELEGATE_VERSION_MATCH_TIMEOUT;
              long heartbeat = Optional.ofNullable((Long) delegateData.get(DELEGATE_HEARTBEAT)).orElse(0L);
              boolean heartbeatTimedOut = now - heartbeat > DELEGATE_HEARTBEAT_TIMEOUT;
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
              boolean shutdownTimedOut = now - shutdownStarted > DELEGATE_SHUTDOWN_TIMEOUT;
              long upgradeStarted =
                  Optional.ofNullable((Long) delegateData.get(DELEGATE_UPGRADE_STARTED)).orElse(Long.MAX_VALUE);
              boolean upgradeTimedOut = now - upgradeStarted > DELEGATE_UPGRADE_TIMEOUT;

              if (multiVersion) {
                if (!expectedVersions.contains(delegateVersion) && !shutdownPending) {
                  drainingNeededList.add(delegateProcess);
                }
              }

              if (newDelegate) {
                logger.info("New delegate process {} is starting", delegateProcess);
                boolean startupTimedOut = now - heartbeat > DELEGATE_STARTUP_TIMEOUT;
                if (startupTimedOut) {
                  newDelegateTimedOut = true;
                  shutdownNeededList.add(delegateProcess);
                }
              } else if (shutdownPending) {
                logger.info(
                    "Shutdown is pending for delegate process {} with version {}", delegateProcess, delegateVersion);
                shutdownPendingList.add(delegateProcess);
                if (shutdownTimedOut || heartbeatTimedOut) {
                  shutdownNeededList.add(delegateProcess);
                }
              } else if (restartNeeded || heartbeatTimedOut || versionMatchTimedOut || delegateMinorVersionMismatch
                  || upgradeTimedOut) {
                logger.info("Restarting delegate process {}. Delegate request: {}, Local heartbeat timeout: {}, "
                        + "Version match timeout: {}, Version banned: {}, Upgrade timeout: {}",
                    delegateProcess, restartNeeded, heartbeatTimedOut, versionMatchTimedOut,
                    delegateMinorVersionMismatch, upgradeTimedOut);
                restartNeededList.add(delegateProcess);
                minMinorVersion.set(0);
                illegalVersions.clear();
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

        if (isNotEmpty(drainingNeededList)) {
          logger.info("Delegate processes {} to be drained.", drainingNeededList);
          drainingNeededList.forEach(this ::drainDelegateProcess);
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
        if (!multiVersion && isNotEmpty(upgradeNeededList)) {
          if (working.compareAndSet(false, true)) {
            logger.info("Delegate processes {} ready for upgrade. Sending confirmation", upgradeNeededList);
            upgradeNeededList.forEach(
                delegateProcess -> messageService.writeMessageToChannel(DELEGATE, delegateProcess, UPGRADING_DELEGATE));
            startDelegateProcess(".", upgradeNeededList, "DelegateUpgradeScript", getProcessId());
          }
        }
      }

      if (multiVersion) {
        for (String version : expectedVersions) {
          if (!runningVersions.contains(version)) {
            if (working.compareAndSet(false, true)) {
              downloadRunScripts(version);
              downloadDelegateJar(version);
              startDelegateProcess(version, emptyList(), "DelegateStartScriptVersioned", getProcessId());
              break;
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error("Error processing delegate stream: {}", e.getMessage(), e);
    }
  }

  private List<String> findExpectedDelegateVersions() {
    try {
      if (multiVersion) {
        RestResponse<DelegateConfiguration> restResponse = timeLimiter.callWithTimeout(
            ()
                -> execute(managerClient.getDelegateConfiguration(watcherConfiguration.getAccountId())),
            15L, TimeUnit.SECONDS, true);
        DelegateConfiguration config = restResponse.getResource();
        return config.getDelegateVersions();
      } else {
        String delegateMetadata = getResponseFromUrl(watcherConfiguration.getDelegateCheckLocation());
        return singletonList(substringBefore(delegateMetadata, " ").trim());
      }
    } catch (UncheckedTimeoutException e) {
      logger.warn("Timed out fetching delegate version information", e);
    } catch (Exception e) {
      logger.warn("Unable to fetch delegate version information", e);
    }
    return emptyList();
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

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  private void downloadRunScripts(String version) {
    if (new File(version + "/delegate.sh").exists()) {
      return;
    }

    try {
      RestResponse<DelegateScripts> restResponse = timeLimiter.callWithTimeout(
          ()
              -> execute(
                  managerClient.downloadRunScripts(version, NEW_DELEGATE_VERSION, watcherConfiguration.getAccountId())),
          1L, TimeUnit.MINUTES, true);
      DelegateScripts delegateScripts = restResponse.getResource();

      Path versionDir = Paths.get(version);
      if (!versionDir.toFile().exists()) {
        Files.createDirectory(versionDir);
      }

      for (String fileName : asList("start.sh", "stop.sh", "delegate.sh")) {
        String filePath = fileName;
        if ("delegate.sh".equals(fileName)) {
          filePath = version + "/" + fileName;
        }
        File scriptFile = new File(filePath);
        String script = delegateScripts.getScriptByName(fileName);

        if (isNotEmpty(script)) {
          Files.deleteIfExists(Paths.get(filePath));
          try (BufferedWriter writer = Files.newBufferedWriter(scriptFile.toPath())) {
            writer.write(script, 0, script.length());
            writer.flush();
          }
          logger.info("Done replacing file [{}]. Set User and Group permission", scriptFile);
          Files.setPosixFilePermissions(scriptFile.toPath(),
              Sets.newHashSet(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE,
                  PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ));
          logger.info("Done setting file permissions");
        } else {
          logger.error("Script for file [{}] was not replaced", scriptFile);
        }
      }
    } catch (Exception e) {
      logger.error("Error downloading run scripts. ", e);
    }
  }

  private void downloadDelegateJar(String version) {
    try {
      String minorVersion = getMinorVersion(version).toString();
      RestResponse<String> restResponse = timeLimiter.callWithTimeout(
          ()
              -> execute(managerClient.getDelegateDownloadUrl(minorVersion, watcherConfiguration.getAccountId())),
          30L, TimeUnit.SECONDS, true);
      String downloadUrl = restResponse.getResource();
      logger.info("Downloading delegate jar version {}", version);
      File destination = new File(version + "/delegate.jar");
      if (destination.exists()) {
        FileUtils.forceDelete(destination);
      }
      InputStream stream = Http.getResponseStreamFromUrl(downloadUrl, httpProxyHost, 600000, 600000);
      FileUtils.copyInputStreamToFile(stream, destination);
    } catch (UncheckedTimeoutException e) {
      logger.error("Timed out downloading delegate jar version {}", version);
    } catch (Exception e) {
      logger.error("Error downloading delegate jar version {}", version, e);
    }
  }

  private void drainDelegateProcess(String delegateProcess) {
    logger.info("Sending old delegate process {} stop-acquiring message", delegateProcess);
    messageService.writeMessageToChannel(DELEGATE, delegateProcess, DELEGATE_STOP_ACQUIRING);
  }

  private void startDelegateProcess(
      String versionFolder, List<String> oldDelegateProcesses, String scriptName, String watcherProcess) {
    if (!new File(versionFolder + "/delegate.sh").exists()) {
      working.set(false);
      return;
    }

    executorService.submit(() -> {
      StartedProcess newDelegate = null;
      try {
        newDelegate = new ProcessExecutor()
                          .timeout(5, TimeUnit.MINUTES)
                          .command("nohup", versionFolder + "/delegate.sh", watcherProcess, versionFolder)
                          .redirectError(Slf4jStream.of(scriptName).asError())
                          .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
                          .start();

        boolean success = false;
        String newDelegateProcess = null;

        if (newDelegate.getProcess().isAlive()) {
          Message message = messageService.waitForMessage(NEW_DELEGATE, TimeUnit.MINUTES.toMillis(4));
          if (message != null) {
            newDelegateProcess = message.getParams().get(0);
            logger.info("Got process ID from new delegate: {}", newDelegateProcess);
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
          logger.error("Watcher messages:");
          messageService.logAllMessages(WATCHER, watcherProcess);
          messageService.clearChannel(WATCHER, watcherProcess);
          if (isNotBlank(newDelegateProcess)) {
            logger.error("Delegate messages:");
            messageService.logAllMessages(DELEGATE, newDelegateProcess);
            messageService.clearChannel(DELEGATE, newDelegateProcess);
          }
          newDelegate.getProcess().destroy();
          newDelegate.getProcess().waitFor();
          oldDelegateProcesses.forEach(oldDelegateProcess -> {
            logger.info("Sending old delegate process {} resume message", oldDelegateProcess);
            messageService.writeMessageToChannel(DELEGATE, oldDelegateProcess, DELEGATE_RESUME);
          });
        }
      } catch (Exception e) {
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
        sleep(ofSeconds(5));
        new ProcessExecutor().timeout(5, TimeUnit.SECONDS).command("kill", "-3", delegateProcess).start();
        sleep(ofSeconds(15));
        new ProcessExecutor().timeout(5, TimeUnit.SECONDS).command("kill", "-9", delegateProcess).start();
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

  private void shutdownWatcher(String watcherProcess) {
    if (!StringUtils.equals(watcherProcess, getProcessId())) {
      logger.warn("Shutting down extra watcher {}", watcherProcess);
      executorService.submit(() -> {
        try {
          new ProcessExecutor().timeout(5, TimeUnit.SECONDS).command("kill", "-9", watcherProcess).start();
        } catch (Exception e) {
          logger.error("Error killing watcher {}", watcherProcess, e);
        }
        messageService.closeChannel(WATCHER, watcherProcess);
        messageService.removeData(WATCHER_DATA, EXTRA_WATCHER);
      });
    }
  }

  private void checkForWatcherUpgrade() {
    if (!watcherConfiguration.isDoUpgrade()) {
      logger.info("Auto upgrade is disabled in watcher configuration");
      logger.info("Watcher stays on version: [{}]", getVersion());
      return;
    }
    try {
      // TODO - if multiVersion use manager endpoint
      String watcherMetadata = getResponseFromUrl(watcherConfiguration.getUpgradeCheckLocation());
      String latestVersion = substringBefore(watcherMetadata, " ").trim();
      boolean upgrade = !StringUtils.equals(getVersion(), latestVersion);
      if (upgrade) {
        logger.info("[Old] Upgrading watcher");
        working.set(true);
        upgradeWatcher(getVersion(), latestVersion);
      } else {
        logger.info("Watcher up to date");
      }
    } catch (Exception e) {
      working.set(false);
      logger.error("Exception while checking for upgrade", e);
      logConfigWatcherYml();
    }
  }

  private void checkForCommands() {
    try {
      String watcherMetadataUrl = watcherConfiguration.getUpgradeCheckLocation();
      String env = watcherMetadataUrl.substring(watcherMetadataUrl.lastIndexOf('/') + 8);
      String watcherCommandsUrl =
          watcherMetadataUrl.substring(0, watcherMetadataUrl.lastIndexOf('/')) + "/commands/" + env;
      String watcherCommands = getResponseFromUrl(watcherCommandsUrl);
      if (isNotBlank(watcherCommands)) {
        BufferedReader reader = new BufferedReader(new StringReader(watcherCommands));
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
          if ("illegalVersion".equals(cmd)) {
            int illegalVersion = Integer.parseInt(param);
            if (!illegalVersions.contains(illegalVersion)) {
              illegalVersions.add(illegalVersion);
              logger.info("Setting illegal delegate version: {}", illegalVersion);
            }
          }
        }
      }
    } catch (IOException | RuntimeException e) {
      logger.info("No commands found. config-watcher.yml:");
      logConfigWatcherYml();
    }
  }

  private static void logConfigWatcherYml() {
    try {
      File configWatcher = new File("config-watcher.yml");
      if (configWatcher.exists()) {
        LineIterator reader = FileUtils.lineIterator(configWatcher);
        while (reader.hasNext()) {
          logger.warn("   " + reader.nextLine());
        }
      }
    } catch (IOException ex) {
      logger.error("Couldn't read config-watcher.yml", ex);
    }
  }

  private String getResponseFromUrl(String url) throws IOException {
    return Http.getResponseStringFromUrl(url, httpProxyHost, 10000, 10000);
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  private void upgradeWatcher(String version, String newVersion) {
    StartedProcess process = null;
    try {
      logger.info("[Old] Starting new watcher");
      process = new ProcessExecutor()
                    .timeout(5, TimeUnit.MINUTES)
                    .command("nohup", "./start.sh", "upgrade", WatcherApplication.getProcessId())
                    .redirectError(Slf4jStream.of("UpgradeScript").asError())
                    .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
                    .start();

      boolean success = false;

      if (process.getProcess().isAlive()) {
        Message message = messageService.waitForMessage(NEW_WATCHER, TimeUnit.MINUTES.toMillis(3));
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
      logger.error("", e);
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
      logger.error(format("Failed to clean watcher version [%s] from Backup", newVersion), ex);
    }
  }

  private void removeWatcherVersionFromCapsule(String version, String newVersion) {
    try {
      cleanup(new File(System.getProperty("capsule.dir")).getParentFile(), version, newVersion, "watcher-");
    } catch (Exception ex) {
      logger.error(format("Failed to clean watcher version [%s] from Capsule", newVersion), ex);
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
