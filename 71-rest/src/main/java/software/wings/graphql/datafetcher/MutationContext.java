package software.wings.graphql.datafetcher;

import graphql.schema.DataFetchingEnvironment;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MutationContext {
  private String accountId;
  private DataFetchingEnvironment dataFetchingEnvironment;
}
