package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;

import java.util.List;
import javax.validation.constraints.NotNull;

@JsonTypeName("BITBUCKET")
@Value
@Builder
public class BitBucketPayloadSource implements PayloadSource {
  @NotNull private Type type = Type.BITBUCKET;
  private List<BitBucketEventType> bitBucketEvents;
  private List<CustomPayloadExpression> customPayloadExpressions;
}
