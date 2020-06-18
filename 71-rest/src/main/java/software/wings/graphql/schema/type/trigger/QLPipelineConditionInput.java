package software.wings.graphql.schema.type.trigger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLPipelineConditionInput {
  private String pipelineId;
}
