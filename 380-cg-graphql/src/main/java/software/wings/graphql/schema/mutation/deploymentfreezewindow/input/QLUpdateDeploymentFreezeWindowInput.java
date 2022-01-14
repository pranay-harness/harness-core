package software.wings.graphql.schema.mutation.deploymentfreezewindow.input;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.mutation.QLMutationInput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLUpdateDeploymentFreezeWindowInput implements QLMutationInput {
  String clientMutationId;
  String id;
  String name;
  String description;
  List<QLFreezeWindowInput> freezeWindows;
  QLSetupInput setup;
  List<String> notifyTo;
}
