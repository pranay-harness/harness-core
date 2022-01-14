package software.wings.graphql.schema.mutation.event.input;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CgEventRule;
import io.harness.beans.WebHookEventConfig;

import software.wings.graphql.schema.mutation.QLMutationInput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLUpdateEventsConfigInput implements QLMutationInput {
  String clientMutationId;
  String appId;
  String name;
  WebHookEventConfig webhookConfig;
  CgEventRule rule;
  List<String> delegateSelectors;
  boolean enabled;
  String eventsConfigId;
}
