package software.wings.service.impl;

import org.mongodb.morphia.Datastore;
import software.wings.beans.Execution;
import software.wings.beans.HostInstanceMapping;
import software.wings.service.intfc.SSHNodeSetExecutorService;

import java.util.concurrent.atomic.AtomicInteger;

public class SSHNodeSetExecutorServiceImpl implements SSHNodeSetExecutorService {
  private AtomicInteger maxSSHConnection;
  private Datastore datastore;

  public SSHNodeSetExecutorServiceImpl(Datastore datastore, int maxSSHConnection) {
    this.datastore = datastore;
    this.maxSSHConnection = new AtomicInteger(maxSSHConnection);
  }
  @Override
  public void execute(Execution execution) {
    for (String hostInstanceMapping : execution.getHostInstanceMappings()) {
      HostInstanceMapping hiMapping = datastore.get(HostInstanceMapping.class, hostInstanceMapping);
      String hostName = hiMapping.getHost().getHostName();
      int sshPort = hiMapping.getHost().getSshPort();
      String sshUser = execution.getSshUser();
      String sshPassword = execution.getSshPassword();
      String command = execution.getCommand();

      //			SSHCommandExecutor executor = new SSHCommandExecutor(hostName, sshPort, sshUser,
      //sshPassword, command, new ConsoleExecutionCallback(execution)); 			executor.execute();
    }
  }
}