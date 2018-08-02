package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.Delegate.Status;

import java.util.List;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class DelegateStatus {
  List<String> publishedVersions;
  List<DelegateInner> delegates;

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @Value
  @Builder
  public static class DelegateInner {
    private String uuid;
    private String ip;
    private String hostName;
    private String delegateName;
    private String description;
    private Status status;
    private long lastHeartBeat;
    private List<DelegateScope> includeScopes;
    private List<DelegateScope> excludeScopes;
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
