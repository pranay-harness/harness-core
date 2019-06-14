package io.harness.delegate.task.executioncapability;

import com.google.inject.Singleton;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.NetCatExecutionCapability;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@Slf4j
@Singleton
public class NetCatCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    NetCatExecutionCapability netCatExecutionCapability = (NetCatExecutionCapability) delegateCapability;
    ProcessExecutor processExecutor =
        new ProcessExecutor().command(netCatExecutionCapability.processExecutorArguments());
    boolean valid = false;
    try {
      final ProcessResult result = processExecutor.execute();
      valid = result.getExitValue() == 0;
    } catch (Exception e) {
      StringBuilder msg = new StringBuilder(128).append("Failed to execute command with arguments, ");
      netCatExecutionCapability.processExecutorArguments().forEach(capability -> msg.append(capability).append(' '));
      logger.error(msg.toString());
    }

    return CapabilityResponse.builder().delegateCapability(netCatExecutionCapability).validated(valid).build();
  }
}
