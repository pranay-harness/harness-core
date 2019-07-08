package software.wings.graphql.datafetcher;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Singleton;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import org.apache.commons.beanutils.PropertyUtils;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.directive.DataFetcherDirective.DataFetcherDirectiveAttributes;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.QLPageInfo.QLPageInfoBuilder;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberOperator;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringOperator;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;

@Singleton
public class DataFetcherUtils {
  public static final String GENERIC_EXCEPTION_MSG = "An error has occurred. Please contact the Harness support team.";
  public static final String NEGATIVE_LIMIT_ARG_MSG = "Limit argument accepts only non negative values";
  public static final String NEGATIVE_OFFSET_ARG_MSG = "Offset argument accepts only non negative values";
  public static final String EXCEPTION_MSG_DELIMITER = ";; ";

  @NotNull
  public Query populateAccountFilter(WingsPersistence wingsPersistence, String accountId, Class entityClass) {
    Query query = wingsPersistence.createQuery(entityClass);
    query.filter(SettingAttributeKeys.accountId, accountId);
    return query;
  }

  public Object getFieldValue(Object obj, String fieldName) {
    try {
      return PropertyUtils.getProperty(obj, fieldName);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
      throw new InvalidRequestException(String.format("Failed to obtain the value for field %s", fieldName), exception);
    }
  }

  public Map<String, String> getContextFieldArgsMap(
      Map<String, DataFetcherDirectiveAttributes> parentToContextFieldArgsMap, String parentTypeName) {
    DataFetcherDirectiveAttributes dataFetcherDirectiveAttributes = parentToContextFieldArgsMap.get(parentTypeName);
    Map<String, String> contextFieldArgsMap = null;
    if (dataFetcherDirectiveAttributes != null) {
      contextFieldArgsMap = dataFetcherDirectiveAttributes.getContextFieldArgsMap();
    }
    return contextFieldArgsMap;
  }

  public String getAccountId(DataFetchingEnvironment environment) {
    GraphQLContext context = null;

    if (environment.getContext() instanceof GraphQLContext) {
      context = environment.getContext();
    } else if (environment.getContext() instanceof GraphQLContext.Builder) {
      GraphQLContext.Builder builder = environment.getContext();
      context = builder.build();
    } else {
      throw new WingsException("Unknown context");
    }

    String accountId = context.get("accountId");

    if (isEmpty(accountId)) {
      throw new WingsException("accountId is null in the environment");
    }

    return accountId;
  }

  public interface Controller<T> { void populate(T entity); }

  public void setStringFilter(FieldEnd<? extends Query<?>> field, QLStringFilter stringFilter) {
    if (stringFilter == null) {
      throw new WingsException("Filter is null");
    }

    QLStringOperator operator = stringFilter.getOperator();
    if (operator == null) {
      throw new WingsException("String Operator cannot be null");
    }

    String[] values = stringFilter.getValues();

    if (isEmpty(values)) {
      throw new WingsException("Values cannot be empty");
    }

    switch (operator) {
      case IN:
        field.in(Arrays.asList(values));
        break;
      case NOT_IN:
        field.notIn(Arrays.asList(values));
        break;
      case EQUALS:
        if (values.length > 1) {
          throw new WingsException("Only one value needs to be inputted for operator EQUALS");
        }
        field.equal(values[0]);
        break;
      case NOT_EQUALS:
        if (values.length > 1) {
          throw new WingsException("Only one value needs to be inputted for operator NOT_EQUALS");
        }
        field.notEqual(values[0]);
        break;

      default:
        throw new WingsException("Unknown String operator " + operator);
    }
  }

  public void setNumberFilter(FieldEnd<? extends Query<?>> field, QLNumberFilter numberFilter) {
    if (numberFilter == null) {
      throw new WingsException("Filter is null");
    }

    QLNumberOperator operator = numberFilter.getOperator();
    if (operator == null) {
      throw new WingsException("Number operator is null");
    }

    Number[] values = numberFilter.getValues();

    if (isEmpty(values)) {
      throw new WingsException("Values cannot be empty");
    }

    switch (operator) {
      case EQUALS:
      case NOT_EQUALS:
      case LESS_THAN:
      case LESS_THAN_OR_EQUALS:
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUALS:
        if (values.length > 1) {
          throw new WingsException("Only one value is expected for operator " + operator);
        }
        break;
      default:
        break;
    }

    switch (operator) {
      case EQUALS:
        field.equal(values[0]);
        break;
      case IN:
        field.in(Arrays.asList(values));
        break;
      case NOT_EQUALS:
        field.notEqual(values[0]);
        break;
      case NOT_IN:
        field.notIn(Arrays.asList(values));
        break;
      case LESS_THAN:
        field.lessThan(values[0]);
        break;
      case LESS_THAN_OR_EQUALS:
        field.lessThanOrEq(values[0]);
        break;
      case GREATER_THAN:
        field.greaterThan(values[0]);
        break;
      case GREATER_THAN_OR_EQUALS:
        field.greaterThanOrEq(values[0]);
        break;
      default:
        throw new WingsException("Unknown Number operator " + operator);
    }
  }

  public <M> QLPageInfo populate(QLPageQueryParameters page, Query<M> query, Controller<M> controller) {
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
}
