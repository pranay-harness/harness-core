package software.wings.graphql.schema.type.audit;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QLApiKeyChangeSet implements QLChangeSet {
  private String id;
  private List<QLChangeDetails> changes;
  private Long triggeredAt;
  private QLRequestInfo request;
  private String failureStatusMsg;
  private String apiKeyId;
}
