package software.wings.delegate.service;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Singleton;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

  @Override
  public void doUpgrade(Delegate delegate, String version) throws IOException, TimeoutException, InterruptedException {
    Files.deleteIfExists(Paths.get("upgrade.sh"));
    File upgradeScript = new File("upgrade.sh");

    try (BufferedWriter writer = Files.newBufferedWriter(upgradeScript.toPath())) {
      writer.write(delegate.getUpgradeScript(), 0, delegate.getUpgradeScript().length());
      writer.flush();
    }

    Files.setPosixFilePermissions(upgradeScript.toPath(),
        Sets.newHashSet(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ));

    StartedProcess process = null;
    try {
      PipedInputStream pipedInputStream = new PipedInputStream();
      process = new ProcessExecutor()
                    .command("nohup", "./upgrade.sh", version)
                    .redirectError(Slf4jStream.of("UpgradeScript").asError())
                    .redirectOutput(Slf4jStream.of("UpgradeScript").asInfo())
                    .redirectOutputAlsoTo(new PipedOutputStream(pipedInputStream))
                    .readOutput(true)
                    .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
                    .start();

      boolean processStarted = timeLimiter.callWithTimeout(
          ()
              -> streamContainsString(new InputStreamReader(pipedInputStream), "Delegate started."),
          10, TimeUnit.MINUTES, false);
      if (processStarted) {
        try {
          signalService.pause();
          signalService.waitForPause();
          new PrintWriter(process.getProcess().getOutputStream(), true).println("StartTasks");
          signalService.stop();
        } finally {
          signalService.resume();
        }
      } else {
        process.getProcess().destroy();
        process.getProcess().waitFor();
      }
    } catch (Exception e) {
      logger.error("Exception while upgrading...", e);
      if (process != null) {
        // Something went wrong restart yourself
        process.getProcess().destroy();
        process.getProcess().waitFor();
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
}
