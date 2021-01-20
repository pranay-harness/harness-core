package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.k8s.model.response.CEK8sDelegatePrerequisite;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@TargetModule(Module._920_DELEGATE_SERVICE_BEANS)
public class CEDelegateStatus {
  private String uuid;
  private Boolean found;
  private Boolean ceEnabled;
  private Long lastHeartBeat;
  private String delegateName;
  private String delegateType;
  private DelegateInstanceStatus status;
  @Builder.Default
  private List<software.wings.beans.DelegateStatus.DelegateInner.DelegateConnectionInner> connections =
      new ArrayList<>();
  private CEK8sDelegatePrerequisite.MetricsServerCheck metricsServerCheck;
  private List<CEK8sDelegatePrerequisite.Rule> permissionRuleList;
}
