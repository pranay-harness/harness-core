package io.harness.delegate.task.executioncapability;

import com.google.inject.Singleton;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@Singleton
@Slf4j
public class ProcessExecutorCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    ProcessExecutorCapability processExecutorCapability = (ProcessExecutorCapability) delegateCapability;

    ProcessExecutor processExecutor =
        new ProcessExecutor().command(processExecutorCapability.getProcessExecutorArguments());
    boolean valid = false;
    try {
      final ProcessResult result = processExecutor.execute();
      valid = result.getExitValue() == 0;
    } catch (Exception e) {
      StringBuilder msg = new StringBuilder(128).append("Failed to execute command with arguments, ");
      processExecutorCapability.getProcessExecutorArguments().forEach(capability -> msg.append(capability).append(' '));
      logger.error(msg.toString());
    }

    return CapabilityResponse.builder().delegateCapability(processExecutorCapability).validated(valid).build();
  }
}
