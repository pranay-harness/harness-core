package software.wings.graphql.datafetcher;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.QLPageInfo.QLPageInfoBuilder;
import software.wings.service.impl.security.auth.AuthHandler;

public abstract class AbstractConnectionDataFetcher<T> extends AbstractDataFetcher<T> {
  @Inject protected HPersistence persistence;

  public AbstractConnectionDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  protected static boolean isPageInfoTotalSelected(DataFetchingEnvironment dataFetchingEnvironment) {
    final DataFetchingFieldSelectionSet selectionSet = dataFetchingEnvironment.getSelectionSet();
    return selectionSet.contains("pageInfo/total");
  }

  protected static boolean isPageInfoHasMoreSelected(DataFetchingEnvironment dataFetchingEnvironment) {
    final DataFetchingFieldSelectionSet selectionSet = dataFetchingEnvironment.getSelectionSet();
    return selectionSet.contains("pageInfo/hasMore");
  }

  public interface Controller<T> { void populate(T entity); }

  protected <T> QLPageInfo populate(QLPageQueryParameters page, Query<T> query,
      DataFetchingEnvironment dataFetchingEnvironment, Controller<T> controller) {
    QLPageInfoBuilder builder = QLPageInfo.builder().limit(page.getLimit()).offset(page.getOffset());

    // A full count of all items that match particular filter could be expensive. This is why using has more feature is
    // recommended over obtaining total. To determine if we have more, we fetch 1 more than the requested.
    final FindOptions options =
        new FindOptions()
            .limit(page.getLimit() + (isPageInfoHasMoreSelected(dataFetchingEnvironment) ? 1 : 0))
            .skip(page.getOffset());

    try (HIterator<T> iterator = new HIterator<T>(query.fetch(options))) {
      int count = 0;
      for (; count < page.getLimit() && iterator.hasNext(); count++) {
        controller.populate(iterator.next());
      }

      if (isPageInfoHasMoreSelected(dataFetchingEnvironment)) {
        builder.hasMore(iterator.hasNext());
      }

      if (isPageInfoTotalSelected(dataFetchingEnvironment)) {
        // If we need total we still have a way to avoid the second query to mongo. If the data we already fetch is all
        // we have, we can calculate the total instead.
        // But not so fast if we did not fetch even a single record, we might of have offset bigger than the amount of
        // data we have.
        // And of course if we did not skip at all we still can owner this result.
        if (iterator.hasNext() || (count == 0 && options.getSkip() > 0)) {
          builder.total((int) query.count());
        } else {
          builder.total(options.getSkip() + count);
        }
      }
    }
    return builder.build();
  }
}
