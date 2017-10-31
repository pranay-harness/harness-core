package software.wings.watcher.service;

import static org.apache.commons.io.filefilter.FileFilterUtils.falseFileFilter;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Singleton;

import com.amazonaws.util.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
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
  @Inject private TimeLimiter timeLimiter;

  @Override
  public void doUpgrade(InputStream newVersionJarStream, String version, String newVersion)
      throws IOException, TimeoutException, InterruptedException {
    // TODO - replace run script

    File watcherJarFile = new File("watcher.jar");
    if (watcherJarFile.exists()) {
      if (!watcherJarFile.delete()) {
        logger.error("Could not delete file.");
      }
    }
    if (!watcherJarFile.createNewFile()) {
      logger.error("Could not create file.");
    }
    IOUtils.copy(newVersionJarStream, new FileOutputStream(watcherJarFile));

    StartedProcess process = null;
    try {
      logger.info("[Old] Upgrading the watcher.");
      PipedInputStream pipedInputStream = new PipedInputStream();
      process = new ProcessExecutor()
                    .timeout(5, TimeUnit.MINUTES)
                    .command("./watch.sh", "upgrade")
                    .redirectError(Slf4jStream.of("UpgradeScript").asError())
                    .redirectOutput(Slf4jStream.of("UpgradeScript").asInfo())
                    .redirectOutputAlsoTo(new PipedOutputStream(pipedInputStream))
                    .readOutput(true)
                    .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
                    .start();
      BufferedReader reader = new BufferedReader(new InputStreamReader(pipedInputStream));
      if (process.getProcess().isAlive() && waitForStringOnStream(reader, "watchstarted", 15)) {
        logger.info("[Old] Watcher upgraded. Stopping.");
        removeWatcherVersionFromCapsule(version, newVersion);
        cleanupOldWatcherVersionFromBackup(version, newVersion);
        watcherService.stop();
      } else {
        logger.error("[Old] Failed to upgrade watcher.");
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
          logger.error("[Old] ALERT: Couldn't kill forcibly.", ex);
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

  private boolean waitForStringOnStream(BufferedReader reader, String searchString, int maxMinutes) {
    try {
      return timeLimiter.callWithTimeout(() -> {
        String line;
        while ((line = reader.readLine()) != null) {
          if (StringUtils.contains(line, searchString)) {
            return true;
          }
        }
        return false;
      }, maxMinutes, TimeUnit.MINUTES, true);
    } catch (Exception e) {
      return false;
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
