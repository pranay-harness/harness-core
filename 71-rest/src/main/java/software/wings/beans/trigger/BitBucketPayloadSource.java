package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;

import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@JsonTypeName("BITBUCKET")
@Value
@Builder
public class BitBucketPayloadSource implements PayloadSource {
  @NotNull private Type type = Type.BITBUCKET;
  public List<BitBucketEventType> bitBucketEvents;
  private List<CustomPayloadExpression> customPayloadExpressions;
}
