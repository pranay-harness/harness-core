package software.wings.service.intfc;

import static software.wings.beans.ErrorConstants.UNKNOWN_COMMAND_UNIT_ERROR;

import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.CopyCommandUnit;
import software.wings.beans.ExecCommandUnit;
import software.wings.beans.Host;
import software.wings.exception.WingsException;

public interface CommandUnitExecutorService {
  default ExecutionResult
    execute(Host host, ExecCommandUnit commandUnit) {
      throw new WingsException(UNKNOWN_COMMAND_UNIT_ERROR);
    }
  default ExecutionResult
    execute(Host host, CopyCommandUnit commandUnit) {
      throw new WingsException(UNKNOWN_COMMAND_UNIT_ERROR);
    }
}
