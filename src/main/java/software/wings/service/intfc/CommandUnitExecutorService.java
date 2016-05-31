package software.wings.service.intfc;

import software.wings.beans.CommandUnit;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.Host;

public interface CommandUnitExecutorService { ExecutionResult execute(Host host, CommandUnit commandUnit); }
