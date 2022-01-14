package software.wings.graphql.schema.mutation.deploymentfreezewindow.input;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.governance.BlackoutWindowFilterType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLFreezeWindowInput {
  List<String> appIds;
  BlackoutWindowFilterType appFilter;
  QLEnvironmentTypeFilterInput envTypeFilter;
  QLServiceTypeFilterInput serviceTypeFilter;
  List<String> envIds;
  List<String> servIds;
}
