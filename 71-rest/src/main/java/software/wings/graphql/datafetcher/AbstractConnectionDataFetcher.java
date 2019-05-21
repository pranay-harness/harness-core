package software.wings.graphql.datafetcher;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.QLPageInfo.QLPageInfoBuilder;

import java.time.OffsetDateTime;

@Slf4j
public abstract class AbstractConnectionDataFetcher<T, P> extends AbstractDataFetcher<T, P> {
  @Inject protected WingsPersistence persistence;

  public interface Controller<T> { void populate(T entity); }

  protected <M> QLPageInfo populate(QLPageQueryParameters page, Query<M> query, Controller<M> controller) {
    QLPageInfoBuilder builder = QLPageInfo.builder().limit(page.getLimit()).offset(page.getOffset());

    // A full count of all items that match particular filter could be expensive. This is why using has more feature is
    // recommended over obtaining total. To determine if we have more, we fetch 1 more than the requested.
    final FindOptions options = new FindOptions().limit(page.getLimit() + 1).skip(page.getOffset());

    try (HIterator<M> iterator = new HIterator<M>(query.fetch(options))) {
      int count = 0;
      for (; count < page.getLimit() && iterator.hasNext(); count++) {
        controller.populate(iterator.next());
      }

      if (page.isHasMoreRequested()) {
        builder.hasMore(iterator.hasNext());
      }

      if (page.isTotalRequested()) {
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

  // Adds closed open range filter to the query
  protected <M> void filterDatetimeRange(Query<M> query, String fieldName, OffsetDateTime from, OffsetDateTime to) {
    if (from != null) {
      query.field(fieldName).greaterThanOrEq(from.toInstant().toEpochMilli());
    }
    if (to != null) {
      query.field(fieldName).lessThan(to.toInstant().toEpochMilli());
    }
  }

  protected abstract T fetchConnection(P parameters);

  @Override
  protected final T fetch(P parameters) {
    try {
      return fetchConnection(parameters);
    } catch (WingsException ex) {
      if (ErrorCode.ACCESS_DENIED.equals(ex.getCode())) {
        logger.warn("User doesn't have access to resource or no entities exist in that app");
        return null;
      }
      throw ex;
    }
  }
}
