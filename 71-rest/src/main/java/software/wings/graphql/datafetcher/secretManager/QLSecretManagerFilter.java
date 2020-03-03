package software.wings.graphql.datafetcher.secretManager;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

@Value
@Builder
public class QLSecretManagerFilter implements EntityFilter {
  QLSecretManagerTypeFilter type;
  QLIdFilter secretManager;
}
