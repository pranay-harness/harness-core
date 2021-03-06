package io.harness.delegate.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.DelegateStatus;
import software.wings.beans.SelectorType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
@TargetModule(Module._920_DELEGATE_SERVICE_BEANS)
public class DelegateGroupDetails {
  private String delegateType;
  private String groupName;
  private String groupHostName;
  private Map<String, SelectorType> groupSelectors;
  private long lastHeartBeat;
  private List<DelegateStatus.DelegateInner> delegates;
}
