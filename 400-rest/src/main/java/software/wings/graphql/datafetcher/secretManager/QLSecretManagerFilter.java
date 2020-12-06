package software.wings.graphql.datafetcher.secretManager;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLSecretManagerFilter implements EntityFilter {
  QLSecretManagerTypeFilter type;
  QLIdFilter secretManager;
}
