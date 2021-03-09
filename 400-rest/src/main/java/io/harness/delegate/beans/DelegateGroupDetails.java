package io.harness.delegate.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.DelegateStatus;
import software.wings.beans.SelectorType;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class DelegateGroupDetails {
  private String delegateType;
  private String groupName;
  private String groupHostName;
  private Map<String, SelectorType> groupSelectors;
  private long lastHeartBeat;
  private List<DelegateStatus.DelegateInner> delegates;
}
