package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.DEL)
public class DelegateGroupListing {
  List<DelegateGroupDetails> delegateGroupDetails;

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @Value
  @Builder
  public static class DelegateInner {
    private String uuid;
    // lastHeartbeat, activelyConnected and hostName is used only in case of NG.
    private long lastHeartbeat;
    private boolean activelyConnected;
    private String hostName;
    List<DelegateConnectionDetails> connections;
  }
}
