package software.wings.graphql.schema.type.audit;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLUser;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLUserChangeSet implements QLChangeSet {
  private String id;
  private List<QLChangeDetails> changes;
  private Long triggeredAt;
  private QLRequestInfo request;
  private String failureStatusMsg;
  private QLUser triggeredBy;
}
