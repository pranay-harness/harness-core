package software.wings.beans;

import software.wings.beans.Delegate.Status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class DelegateStatus {
  List<String> publishedVersions;
  List<DelegateInner> delegates;
  List<DelegateScalingGroup> scalingGroups;

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @Value
  @Builder
  public static class DelegateInner {
    private String uuid;
    private String ip;
    private String hostName;
    private String delegateName;
    private String delegateGroupName;
    private String description;
    private Status status;
    private long lastHeartBeat;
    private boolean activelyConnected;
    private String delegateProfileId;
    private String delegateType;
    private boolean polllingModeEnabled;
    private boolean proxy;
    private boolean ceEnabled;
    private List<DelegateScope> includeScopes;
    private List<DelegateScope> excludeScopes;
    private List<String> tags;
    private Map<String, SelectorType> implicitSelectors;
    private long profileExecutedAt;
    private boolean profileError;
    private boolean sampleDelegate;
    List<DelegateConnectionInner> connections;

    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Value
    @Builder
    public static class DelegateConnectionInner {
      private String uuid;
      private String version;
      private long lastHeartbeat;
    }
  }
}
