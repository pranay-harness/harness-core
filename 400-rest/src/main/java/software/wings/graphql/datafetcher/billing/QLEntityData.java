package software.wings.graphql.datafetcher.billing;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLEntityData {
  private String name;
  private String id;
  private String type;
}
