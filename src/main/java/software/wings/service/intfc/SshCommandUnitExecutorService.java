package software.wings.service.intfc;

import software.wings.beans.Execution;
import software.wings.core.ssh.executors.SshExecutor.ExecutionResult;

public interface SshCommandUnitExecutorService { ExecutionResult execute(Execution execution); }
