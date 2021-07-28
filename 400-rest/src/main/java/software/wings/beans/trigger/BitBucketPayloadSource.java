package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.trigger.WebhookSource.BitBucketEventType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@JsonTypeName("BITBUCKET")
@Value
@Builder
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class BitBucketPayloadSource implements PayloadSource {
  @NotNull private Type type = Type.BITBUCKET;
  public List<BitBucketEventType> bitBucketEvents;
  private List<CustomPayloadExpression> customPayloadExpressions;
}
