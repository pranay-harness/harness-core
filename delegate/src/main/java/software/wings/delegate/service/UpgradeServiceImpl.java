package software.wings.delegate.service;

import static java.util.Arrays.asList;
import static org.apache.commons.io.filefilter.FileFilterUtils.falseFileFilter;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import software.wings.beans.DelegateScripts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 1/4/17.
 */
@Singleton
public class UpgradeServiceImpl implements UpgradeService {
  private static final Logger logger = LoggerFactory.getLogger(UpgradeServiceImpl.class);

  @Inject private TimeLimiter timeLimiter;
  @Inject private DelegateService delegateService;
  @Inject private SignalService signalService;

  @Override
  public void doUpgrade(DelegateScripts delegateScripts, String version)
      throws IOException, TimeoutException, InterruptedException {
    logger.info("[Old] Replace run scripts");
    replaceRunScripts(delegateScripts);
    logger.info("[Old] Run scripts downloaded");

    File goaheadFile = new File("goahead");
    StartedProcess process = null;
    try {
      logger.info("[Old] Starting new delegate process");
      PipedInputStream pipedInputStream = new PipedInputStream();
      process = new ProcessExecutor()
                    .timeout(5, TimeUnit.MINUTES)
                    .command("nohup", "./upgrade.sh", version)
                    .redirectError(Slf4jStream.of("UpgradeScript").asError())
                    .redirectOutput(Slf4jStream.of("UpgradeScript").asInfo())
                    .redirectOutputAlsoTo(new PipedOutputStream(pipedInputStream))
                    .readOutput(true)
                    .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
                    .start();
      logger.info("[Old] Upgrade script executed: {}", process.getProcess().isAlive());

      logger.info("[Old] Waiting for process to start.");
      boolean processStarted = timeLimiter.callWithTimeout(
          ()
              -> streamContainsString(new InputStreamReader(pipedInputStream), "botstarted"),
          15, TimeUnit.MINUTES, false);
      if (processStarted) {
        try {
          logger.info("[Old] New delegate process started.");
          while (delegateService.getRunningTaskCount() > 0) {
            logger.info("[Old] Delegate blocking new delegate while completing {} tasks.",
                delegateService.getRunningTaskCount());
            Thread.sleep(1000);
          }
          logger.info("[Old] Delegate finished with tasks, pausing.");
          signalService.pause();
          logger.info("[Old] Delegate paused.");

          if (goaheadFile.createNewFile()) {
            logger.info("[Old] Sent go ahead to new delegate.");
            boolean goaheadReceived = timeLimiter.callWithTimeout(
                ()
                    -> streamContainsString(new InputStreamReader(pipedInputStream), "proceeding"),
                15, TimeUnit.MINUTES, false);

            if (goaheadReceived) {
              logger.info("[Old] Handshake complete with new delegate. Shutting down.");
              removeDelegateVersionFromCapsule(delegateScripts, version);
              cleanupOldDelegateVersionFromBackup(delegateScripts, version);

              signalService.stop();
            } else {
              process.getProcess().destroy();
              process.getProcess().waitFor();
            }
          } else {
            logger.error("[Old] Could not create go ahead file.");
          }
        } finally {
          if (!goaheadFile.delete()) {
            logger.error("[Old] Could not delete go ahead file.");
          }
          signalService.resume();
        }
      } else {
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

  @Override
  public void doRestart() throws IOException, TimeoutException, InterruptedException {
    StartedProcess process = null;
    try {
      logger.info("[Old] Restarting the delegate.");
      signalService.pause();
      logger.info("[Old] Previous delegate paused.");
      PipedInputStream pipedInputStream = new PipedInputStream();
      process = new ProcessExecutor()
                    .timeout(5, TimeUnit.MINUTES)
                    .command("./run.sh", "restart")
                    .redirectError(Slf4jStream.of("RestartScript").asError())
                    .redirectOutput(Slf4jStream.of("RestartScript").asInfo())
                    .redirectOutputAlsoTo(new PipedOutputStream(pipedInputStream))
                    .readOutput(true)
                    .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
                    .start();
      try {
        if (process.getProcess().isAlive()) {
          logger.info("[Old] New delegate restarted. Stopping.");
          signalService.stop();
        } else {
          logger.error("[Old] Failed to restart delegate.");
        }
      } finally {
        signalService.resume();
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("[Old] Exception while restarting", e);
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

  private void cleanupOldDelegateVersionFromBackup(DelegateScripts delegateScripts, String version) {
    try {
      cleanup(new File(System.getProperty("user.dir")), version, delegateScripts.getVersion(), "backup.");
    } catch (Exception ex) {
      logger.error(
          String.format("Failed to clean delegate version [%s] from Backup", delegateScripts.getVersion()), ex);
    }
  }

  private void removeDelegateVersionFromCapsule(DelegateScripts delegateScripts, String version) {
    try {
      cleanup(new File(System.getProperty("capsule.dir")).getParentFile(), version, delegateScripts.getVersion(),
          "delegate-");
    } catch (Exception ex) {
      logger.error(
          String.format("Failed to clean delegate version [%s] from Capsule", delegateScripts.getVersion()), ex);
    }
  }

  private void replaceRunScripts(DelegateScripts delegateScripts) throws IOException {
    for (String fileName : asList("upgrade.sh", "run.sh", "stop.sh")) {
      Files.deleteIfExists(Paths.get(fileName));
      File scriptFile = new File(fileName);
      String script = delegateScripts.getScriptByName(fileName);

      if (script != null && script.length() != 0) {
        try (BufferedWriter writer = Files.newBufferedWriter(scriptFile.toPath())) {
          writer.write(script, 0, script.length());
          writer.flush();
        }
        logger.info("[Old] Done replacing file [{}]. Set User and Group permission", scriptFile);
        Files.setPosixFilePermissions(scriptFile.toPath(),
            Sets.newHashSet(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ));
        logger.info("[Old] Done setting file permissions");
      } else {
        logger.error("[Old] Couldn't find script for file [{}]", scriptFile);
      }
    }
  }

  private boolean streamContainsString(Reader reader, String searchString) throws IOException {
    char[] buffer = new char[1024];
    int numCharsRead;
    int count = 0;
    while ((numCharsRead = reader.read(buffer)) > 0) {
      for (int c = 0; c < numCharsRead; c++) {
        if (buffer[c] == searchString.charAt(count))
          count++;
        else
          count = 0;
        if (count == searchString.length())
          return true;
      }
    }
    return false;
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
