package software.wings.graphql.schema.type.instance.info;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class QLPcfInstanceInfo extends QLInstanceInfo {
  private String id;
  private String organization;
  private String space;
  private String pcfApplicationName;
  private String pcfApplicationGuid;
  private String instanceIndex;
}
