package software.wings.graphql.datafetcher;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import io.harness.persistence.HPersistence;
import software.wings.service.impl.security.auth.AuthHandler;

public abstract class AbstractConnectionDataFetcher<T> extends AbstractDataFetcher<T> {
  protected static final String LIMIT_ARG = "limit";
  protected static final String OFFSET_ARG = "offset";

  @Inject protected HPersistence persistence;

  public AbstractConnectionDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  protected static boolean isPageInfoTotalSelected(DataFetchingEnvironment dataFetchingEnvironment) {
    final DataFetchingFieldSelectionSet selectionSet = dataFetchingEnvironment.getSelectionSet();
    return selectionSet.contains("pageInfo/total");
  }
}
