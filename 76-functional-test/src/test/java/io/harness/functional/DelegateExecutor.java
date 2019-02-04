package io.harness.functional;

import static io.harness.generator.AccountGenerator.Accounts.GENERIC_TEST;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.utils.Strings;
import io.harness.filesystem.FileIo;
import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.resource.Project;
import io.harness.threading.Puller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import software.wings.beans.Account;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateStatus;
import software.wings.beans.DelegateStatus.DelegateInner;
import software.wings.service.intfc.DelegateService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Singleton
public class DelegateExecutor {
  private static boolean failedAlready;
  private static final Logger logger = LoggerFactory.getLogger(DelegateExecutor.class);

  @Inject private DelegateService delegateService;
  @Inject private AccountGenerator accountGenerator;
  @Inject private OwnerManager ownerManager;
  private String accountId;

  public void ensureDelegate() throws IOException {
    final Seed seed = new Seed(0);
    Owners owners = ownerManager.create();

    Account account = accountGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    accountId = account.getUuid();
    if (!isHealthy(account.getUuid())) {
      executeLocalDelegate();
    }
  }

  private void executeLocalDelegate() throws IOException {
    if (failedAlready) {
      return;
    }

    String directoryPath = Project.rootDirectory(AbstractFunctionalTest.class);
    final File directory = new File(directoryPath);
    final File lockfile = new File(directoryPath, "delegate");

    if (FileIo.acquireLock(lockfile, ofMinutes(2))) {
      try {
        if (isHealthy(accountId)) {
          return;
        }
        logger.info("Execute the delegate from {}", directory);
        final Path jar = Paths.get(directory.getPath(), "81-delegate", "target", "delegate-capsule.jar");
        final Path config = Paths.get(directory.getPath(), "81-delegate", "config-delegate.yml");

        final List<String> command = asList("java", "-Xmx4096m", "-XX:+HeapDumpOnOutOfMemoryError",
            "-XX:+PrintGCDetails", "-XX:+PrintGCDateStamps", "-Xloggc:mygclogfilename.gc", "-XX:+UseParallelGC",
            "-XX:MaxGCPauseMillis=500", "-jar", jar.toString(), config.toString());

        logger.info(Strings.join(command, " "));

        ProcessExecutor processExecutor = new ProcessExecutor();
        processExecutor.directory(directory);
        processExecutor.command(command);

        processExecutor.redirectOutput(System.out);
        processExecutor.redirectError(System.err);
        //        processExecutor.redirectOutput(null);
        //        processExecutor.redirectError(null);

        processExecutor.start();

        Puller.pullFor(ofMinutes(2), () -> isHealthy(accountId));

      } catch (RuntimeException exception) {
        failedAlready = true;
        throw exception;
      } finally {
        FileIo.releaseLock(lockfile);
      }
    }
  }

  private boolean isHealthy(String accountId) {
    // TODO move to api call from manager to check delegate up and registered
    try {
      DelegateStatus delegateStatus = delegateService.getDelegateStatus(accountId);

      if (!delegateStatus.getDelegates().isEmpty()) {
        for (DelegateInner delegateInner : delegateStatus.getDelegates()) {
          long lastMinuteMillis = System.currentTimeMillis() - 60000;
          if (delegateInner.getStatus().equals(Status.ENABLED) && delegateInner.getLastHeartBeat() > lastMinuteMillis) {
            return true;
          }
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }
}
