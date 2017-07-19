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
import software.wings.beans.Delegate;
import software.wings.managerclient.ManagerClient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 1/4/17.
 */
@Singleton
public class UpgradeServiceImpl implements UpgradeService {
  private static final Logger logger = LoggerFactory.getLogger(UpgradeServiceImpl.class);

  @Inject private ManagerClient managerClient;

  @Inject private TimeLimiter timeLimiter;

  @Inject private SignalService signalService;

  private AtomicBoolean isUpgrading = new AtomicBoolean(false);

  @Override
  public void doUpgrade(Delegate delegate, String version) throws IOException, TimeoutException, InterruptedException {
    logger.info("Replace run scripts");
    replaceRunScripts(delegate);
    logger.info("Run scripts downloaded");

    StartedProcess process = null;
    try {
      logger.info("Starting new delegate process");
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
      logger.info("upgrade script executed " + process.getProcess().isAlive());

      logger.info("Wait for process to start");
      boolean processStarted = timeLimiter.callWithTimeout(
          ()
              -> streamContainsString(new InputStreamReader(pipedInputStream), "botstarted"),
          30, TimeUnit.MINUTES, false);
      if (processStarted) {
        try {
          logger.info("New Delegate started. Pause old delegate");
          signalService.pause();
          logger.info("Old delegate paused");
          new PrintWriter(process.getProcess().getOutputStream(), true).println("goahead");
          removeDelegateVersionFromCapsule(delegate, version);
          cleanupOldDelegateVersionFromBackup(delegate, version);

          signalService.stop();
        } finally {
          signalService.resume();
        }
      } else {
        process.getProcess().destroy();
        process.getProcess().waitFor();
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("Exception while upgrading: " + e.getMessage(), e);
      Arrays.stream(e.getStackTrace()).forEach(elem -> logger.error("Trace: {}", elem));
      if (process != null) {
        // Something went wrong restart yourself
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
          logger.error("ALERT: Couldn't kill forcibly.");
        }
      }
    }
  }

  private void cleanupOldDelegateVersionFromBackup(Delegate delegate, String version) {
    try {
      cleanup(new File(System.getProperty("user.dir")), version, delegate.getVersion(), "backup.");
    } catch (Exception ex) {
      logger.error("Failed to clean delegate version [{}] from Backup", delegate.getVersion());
    }
  }

  private void removeDelegateVersionFromCapsule(Delegate delegate, String version) {
    try {
      cleanup(new File(System.getProperty("capsule.dir")).getParentFile(), version, delegate.getVersion(), "delegate-");
    } catch (Exception ex) {
      logger.error("Failed to clean delegate version [{}] from Capsule", delegate.getVersion());
    }
  }

  private void replaceRunScripts(Delegate delegate) throws IOException {
    for (String fileName : asList("upgrade.sh", "run.sh", "stop.sh")) {
      Files.deleteIfExists(Paths.get(fileName));
      File scriptFile = new File(fileName);
      String script = delegate.getScriptByName(fileName);

      if (script != null && script.length() != 0) {
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
        logger.error("Couldn't find script for file [{}]", scriptFile);
      }
    }
  }

  private boolean streamContainsString(Reader reader, String searchString) throws IOException {
    char[] buffer = new char[1024];
    int numCharsRead;
    int count = 0;
    while ((numCharsRead = reader.read(buffer)) > 0) {
      logger.info("String on stream [{}]", new String(buffer));
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
        logger.info("File Name to be deleted = " + file.getAbsolutePath());
        FileUtils.deleteQuietly(file);
      }
    });
  }
}
