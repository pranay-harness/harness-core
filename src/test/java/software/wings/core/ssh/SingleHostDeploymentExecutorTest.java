package software.wings.core.ssh;

import org.junit.Test;
import org.omg.CORBA.StringHolder;
import software.wings.core.ssh.executors.SSHExecutor;
import software.wings.core.ssh.executors.SSHExecutor.ExecutionResult;
import software.wings.core.ssh.executors.SSHSessionConfig;

import static org.junit.Assert.*;
import static software.wings.common.UUIDGenerator.getUUID;
import static software.wings.core.ssh.executors.SSHExecutor.ExecutorType.PASSWORD;

/**
 * Created by anubhaw on 2/24/16.
 */
public class SingleHostDeploymentExecutorTest {
  @Test
  public void testDeploy() throws Exception {
    SSHSessionConfig config = new SSHSessionConfig.SSHSessionConfigBuilder()
                                  .executionType(PASSWORD)
                                  .executionID(getUUID())
                                  .SSHConnectionTimeout(100000)
                                  .SSHSessionTimeout(100000)
                                  .host("192.168.137.108")
                                  .port(22)
                                  .user("osboxes")
                                  .password("osboxes.org")
                                  .build();

    String appHome = "/home/osboxes/wings/";
    String downloadDir = appHome + "downloads/";
    String appDir = appHome + "app/";

    String setupCommand = String.format("mkdir -p %s && mkdir -p %s", downloadDir, appDir);
    String localFile = "/Users/anubhaw/app.tar.gz";
    String remoteFile = downloadDir + "app.tar.gz";
    String deployCommand = String.format("cd %s && tar -xvzf %s && sh %s", appDir, remoteFile, "app.py");
    System.out.println(deployCommand);

    SingleHostDeploymentExecutor singleHostDeploymentExecutor =
        new SingleHostDeploymentExecutor(setupCommand, localFile, remoteFile, deployCommand, config);
    ExecutionResult result = singleHostDeploymentExecutor.deploy();
    System.out.println(result);
  }
}