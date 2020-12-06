package software.wings.graphql.schema.type.audit;

import software.wings.graphql.schema.type.QLUser;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLUserChangeSet implements QLChangeSet {
  private String id;
  private List<QLChangeDetails> changes;
  private Long triggeredAt;
  private QLRequestInfo request;
  private String failureStatusMsg;
  private QLUser triggeredBy;
}
