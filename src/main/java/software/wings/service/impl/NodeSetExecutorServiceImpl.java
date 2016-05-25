package software.wings.service.impl;

import software.wings.app.WingsBootstrap;
import software.wings.beans.Execution;
import software.wings.service.intfc.NodeSetExecutorService;
import software.wings.service.intfc.SshCommandUnitExecutorService;

public class NodeSetExecutorServiceImpl implements NodeSetExecutorService {
  @Override
  public void execute(Execution execution) {
    WingsBootstrap.lookup(SshCommandUnitExecutorService.class).execute(execution);
  }
}
