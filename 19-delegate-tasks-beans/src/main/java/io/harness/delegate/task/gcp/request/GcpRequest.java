package io.harness.delegate.task.gcp.request;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.ImmutableSet;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public abstract class GcpRequest implements ExecutionCapabilityDemander {
  public enum RequestType { VALIDATE; }

  private String delegateSelector;
  @NotNull private RequestType requestType;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    if (isNotBlank(delegateSelector)) {
      return Collections.singletonList(
          SelectorCapability.builder().selectors(ImmutableSet.of(delegateSelector)).build());
    }
    return Collections.emptyList();
  }
}
