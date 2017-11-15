package software.wings.watcher.service;

import static com.google.common.collect.Iterables.isEmpty;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static software.wings.watcher.app.WatcherApplication.getProcessId;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Singleton;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import software.wings.utils.message.Message;
import software.wings.utils.message.MessageService;
import software.wings.utils.message.MessengerType;
import software.wings.watcher.app.WatcherConfiguration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
  private static final String DELEGATE = "delegate-";

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Object waiter = new Object();

  @Inject @Named("inputExecutor") private ScheduledExecutorService inputExecutor;
  @Inject @Named("watchExecutor") private ScheduledExecutorService watchExecutor;
  @Inject private ExecutorService executorService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private Clock clock;
  @Inject private UpgradeService upgradeService;
  @Inject private WatcherConfiguration watcherConfiguration;
  @Inject private MessageService messageService;
  @Inject private AmazonS3Client amazonS3Client;

  private BlockingQueue<Message> watcherMessages = new ArrayBlockingQueue<>(100);
  private boolean working;
  private List<String> runningDelegates;

  @Override
  public void run(boolean upgrade) {
    try {
      logger.info(upgrade ? "[New] Upgraded watcher process started" : "Watcher process started");
      runningDelegates = Optional.ofNullable((List) messageService.getData("watcher-data", "running-delegates"))
                             .orElse(new ArrayList<>());
      messageService.writeMessage("watcher-started");
      startInputCheck();

      if (upgrade) {
        Message message = waitForIncomingMessage("go-ahead", TimeUnit.MINUTES.toMillis(5));
        logger.info(message != null ? "[New] Got go-ahead. Proceeding"
                                    : "[New] Timed out waiting for go-ahead. Proceeding anyway");
      }

      startWatching();

      logger.info(upgrade ? "[New] Watcher upgraded" : "Watcher started");

      synchronized (waiter) {
        waiter.wait();
      }

    } catch (Exception e) {
      logger.error("Exception while running watcher", e);
    }
  }

  @Override
  public void stop() {
    synchronized (waiter) {
      waiter.notify();
    }
  }

  @Override
  public void resume() {
    working = false;
  }

  @Override
  public Message waitForIncomingMessage(String messageName, long timeout) {
    try {
      return timeLimiter.callWithTimeout(() -> {
        Message message = null;
        while (message == null || !message.getMessage().equals(messageName)) {
          try {
            message = watcherMessages.take();
            logger.info("Message on watcher input queue: " + message);
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
      Message message = messageService.readMessage(TimeUnit.MINUTES.toMillis(1));
      if (message != null) {
        while (!watcherMessages.offer(message)) {
          try {
            Thread.sleep(100L);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }, 0, 1, TimeUnit.SECONDS);
  }

  private void startWatching() {
    watchExecutor.scheduleWithFixedDelay(() -> {
      if (!working) {
        checkForUpgrade();
      }
      if (!working) {
        watchDelegate();
      }
    }, 0, 10, TimeUnit.SECONDS);
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
        upgradeService.upgradeWatcher(newVersionJarObj.getObjectContent(), getVersion(), latestVersion);
      } else {
        logger.info("Watcher up to date");
      }
    } catch (Exception e) {
      working = false;
      logger.error("[Old] Exception while checking for upgrade", e);
    }
  }

  private void watchDelegate() {
    try {
      messageService.listDataNames(DELEGATE)
          .stream()
          .map(s -> s.substring(DELEGATE.length()))
          .filter(s -> !runningDelegates.contains(s))
          .forEach(process -> messageService.closeData(process));

      if (isEmpty(runningDelegates)) {
        working = true;
        startDelegate();
      } else {
        List<String> obsolete = new ArrayList<>();
        for (String delegateProcess : runningDelegates) {
          Map<String, Object> delegateData = messageService.getAllData(DELEGATE + delegateProcess);
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
              messageService.putData(DELEGATE + delegateProcess, "shutdownPending", true);
              messageService.putData(DELEGATE + delegateProcess, "shutdownStarted", clock.millis());
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
          Message message = waitForIncomingMessage("new-delegate", TimeUnit.MINUTES.toMillis(2));
          if (message != null) {
            String newDelegateProcess = message.getParams().get(0);
            logger.info("Got process ID from new delegate: " + newDelegateProcess);
            message = messageService.retrieveMessage(
                MessengerType.DELEGATE, newDelegateProcess, TimeUnit.MINUTES.toMillis(2));
            if (message != null && message.getMessage().equals("delegate-started")) {
              if (oldDelegateProcess != null) {
                messageService.sendMessage(MessengerType.DELEGATE, oldDelegateProcess, "stop-acquiring");
              }
              messageService.sendMessage(MessengerType.DELEGATE, newDelegateProcess, "go-ahead");
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
        messageService.closeData(DELEGATE + delegateProcess);
        runningDelegates.remove(delegateProcess);
        messageService.putData("watcher-data", "running-delegates", runningDelegates);
      } catch (Exception e) {
        logger.error("Error killing delegate {}", delegateProcess, e);
      } finally {
        working = false;
      }
    });
  }

  private String getVersion() {
    return System.getProperty("version", "1.0.0-DEV");
  }
}
