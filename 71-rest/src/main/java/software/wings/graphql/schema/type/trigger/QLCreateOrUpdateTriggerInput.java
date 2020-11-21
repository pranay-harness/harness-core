package software.wings.graphql.schema.type.trigger;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLCreateOrUpdateTriggerInput {
  private String triggerId;
  private String clientMutationId;
  private String name;
  private String applicationId;
  private String description;
  private QLTriggerConditionInput condition;
  private QLTriggerActionInput action;
}
