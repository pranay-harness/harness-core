package software.wings.graphql.schema.type.audit;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLChangeSetKeys")
public class QLGenericChangeSet implements QLChangeSet {
  private String id;
  private List<QLChangeDetails> changes;
  private Long triggeredAt;
  private QLRequestInfo request;
  private String failureStatusMsg;
}
