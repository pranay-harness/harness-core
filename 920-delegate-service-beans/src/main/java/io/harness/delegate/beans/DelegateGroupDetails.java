package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.SelectorType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DEL)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class DelegateGroupDetails {
  private String groupId;
  private String delegateGroupIdentifier;
  private String delegateType;
  private String groupName;
  private String groupHostName;
  private String delegateDescription;
  private String delegateConfigurationId;
  private DelegateSizeDetails sizeDetails;
  private Map<String, SelectorType> groupImplicitSelectors;
  private Set<String> groupCustomSelectors;
  private DelegateInsightsDetails delegateInsightsDetails;
  private long lastHeartBeat;
  private boolean activelyConnected;
  private List<DelegateGroupListing.DelegateInner> delegateInstanceDetails;
}
