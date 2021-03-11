package software.wings.graphql.schema.query;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLInfraDefConnectionQueryParameters {
  private String InfrastructureId;
  private String EnvironmentId;
}
