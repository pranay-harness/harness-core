package io.harness.testframework.framework;

import static io.harness.testframework.framework.utils.ExecutorUtils.addConfig;
import static io.harness.testframework.framework.utils.ExecutorUtils.addGCVMOptions;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJacocoAgentVM;
import static io.harness.testframework.framework.utils.ExecutorUtils.addJar;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.Delegate.DelegateKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.utils.Strings;
import io.harness.filesystem.FileIo;
import io.harness.resource.Project;
import io.harness.rest.RestResponse;
import io.harness.threading.Poller;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.GenericType;

@Singleton
@Slf4j
public class DelegateExecutor {
  private static boolean failedAlready;

  @Inject private DelegateService delegateService;

  public void ensureDelegate(Account account, String bearerToken, Class clazz) throws IOException {
    if (!isHealthy(account.getUuid(), bearerToken)) {
      executeLocalDelegate(account, bearerToken, clazz);
    }
  }

  private void executeLocalDelegate(Account account, String bearerToken, Class clazz) throws IOException {
    if (failedAlready) {
      return;
    }

    String directoryPath = Project.rootDirectory(clazz);
    final File directory = new File(directoryPath);
    final File lockfile = new File(directoryPath, "delegate");

    if (FileIo.acquireLock(lockfile, ofMinutes(2))) {
      try {
        if (isHealthy(account.getUuid(), bearerToken)) {
          return;
        }
        logger.info("Execute the delegate from {}", directory);
        final Path jar = Paths.get(directory.getPath(), "81-delegate", "target", "delegate-capsule.jar");
        final Path config = Paths.get(directory.getPath(), "81-delegate", "config-delegate.yml");

        List<String> command = new ArrayList<>();
        command.add("java");

        addGCVMOptions(command);
        addJacocoAgentVM(jar, command);

        addJar(jar, command);
        addConfig(config, command);

        logger.info(Strings.join(command, " "));

        ProcessExecutor processExecutor = new ProcessExecutor();
        processExecutor.directory(directory);
        processExecutor.command(command);

        processExecutor.redirectOutput(System.out);
        processExecutor.redirectError(System.err);
        //        processExecutor.redirectOutput(null);
        //        processExecutor.redirectError(null);

        processExecutor.start();

        Poller.pollFor(ofMinutes(2), ofSeconds(2), () -> isHealthy(account.getUuid(), bearerToken));

      } catch (RuntimeException exception) {
        failedAlready = true;
        throw exception;
      } finally {
        FileIo.releaseLock(lockfile);
      }
    }
  }

  private boolean isHealthy(String accountId, String bearerToken) {
    try {
      RestResponse<DelegateStatus> delegateStatusResponse =
          Setup.portal()
              .auth()
              .oauth2(bearerToken)
              .queryParam(DelegateKeys.accountId, accountId)
              .get("/setup/delegates/status")
              .as(new GenericType<RestResponse<DelegateStatus>>() {}.getType());

      DelegateStatus delegateStatus = delegateStatusResponse.getResource();
      if (!delegateStatus.getDelegates().isEmpty()) {
        for (DelegateInner delegateInner : delegateStatus.getDelegates()) {
          long lastMinuteMillis = System.currentTimeMillis() - 60000;
          if (delegateInner.getStatus() == Status.ENABLED && delegateInner.getLastHeartBeat() > lastMinuteMillis) {
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
