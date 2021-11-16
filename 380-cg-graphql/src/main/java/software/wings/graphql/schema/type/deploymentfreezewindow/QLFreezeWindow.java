package software.wings.graphql.schema.type.deploymentfreezewindow;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.governance.BlackoutWindowFilterType;
import io.harness.governance.EnvironmentFilter.EnvironmentFilterType;
import io.harness.governance.ServiceFilter.ServiceFilterType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLFreezeWindow {
  List<String> appIds;
  BlackoutWindowFilterType appFilter;
  EnvironmentFilterType envFilterType;
  ServiceFilterType servFilterType;
  List<String> envIds;
  List<String> servIds;
}
