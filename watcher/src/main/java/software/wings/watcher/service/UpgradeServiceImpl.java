package software.wings.watcher.service;

import static org.apache.commons.io.filefilter.FileFilterUtils.falseFileFilter;
import static software.wings.utils.message.MessengerType.WATCHER;

import com.google.inject.Singleton;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;

/**
 * Created by brett on 10/27/17
 */
@Singleton
public class UpgradeServiceImpl implements UpgradeService {
  private static final Logger logger = LoggerFactory.getLogger(UpgradeServiceImpl.class);

  @Inject private WatcherService watcherService;
  @Inject private MessageService messageService;

  @Override
  public void upgradeWatcher(InputStream newVersionJarStream, String version, String newVersion)
      throws IOException, TimeoutException, InterruptedException {
    File watcherJarFile = new File("watcher.jar");
    FileUtils.copyInputStreamToFile(newVersionJarStream, watcherJarFile);

    StartedProcess process = null;
    try {
      logger.info("[Old] Upgrading the watcher");
      process = new ProcessExecutor()
                    .timeout(5, TimeUnit.MINUTES)
                    .command("./watch.sh", "upgrade", WatcherApplication.getProcessId())
                    .redirectError(Slf4jStream.of("UpgradeScript").asError())
                    .redirectOutput(Slf4jStream.of("UpgradeScript").asInfo())
                    .readOutput(true)
                    .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
                    .start();

      boolean success = false;

      if (process.getProcess().isAlive()) {
        Message message = watcherService.waitForIncomingMessage("new-watcher", TimeUnit.MINUTES.toMillis(2));
        if (message != null) {
          String newWatcherProcessId = message.getParams().get(0);
          logger.info("[Old] Got process ID from new watcher: " + newWatcherProcessId);
          message = messageService.retrieveMessage(WATCHER, newWatcherProcessId, TimeUnit.MINUTES.toMillis(2));
          if (message != null && message.getMessage().equals("watcher-started")) {
            messageService.sendMessage(WATCHER, newWatcherProcessId, "go-ahead");
            logger.info("[Old] Watcher upgraded. Stopping");
            removeWatcherVersionFromCapsule(version, newVersion);
            cleanupOldWatcherVersionFromBackup(version, newVersion);
            success = true;
            watcherService.stop();
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
}
