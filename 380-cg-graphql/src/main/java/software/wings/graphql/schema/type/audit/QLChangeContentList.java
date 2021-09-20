package software.wings.graphql.schema.type.audit;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(ResourceType.APPLICATION)
@OwnedBy(HarnessTeam.PL)
public class QLChangeContentList {
  private List<QLChangeContent> data;
}
