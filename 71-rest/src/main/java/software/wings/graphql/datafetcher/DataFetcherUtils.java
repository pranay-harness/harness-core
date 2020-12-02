package software.wings.graphql.datafetcher;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.beans.FeatureName;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.app.MainConfiguration;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.directive.DataFetcherDirective.DataFetcherDirectiveAttributes;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLEnum;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.QLPageInfo.QLPageInfoBuilder;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberOperator;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.resources.graphql.TriggeredByType;
import software.wings.service.intfc.FeatureFlagService;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;

@Slf4j
@Singleton
public class DataFetcherUtils {
  public static final String GENERIC_EXCEPTION_MSG = "An error has occurred. Please contact the Harness support team.";
  public static final String NEGATIVE_LIMIT_ARG_MSG = "Limit argument accepts only non negative values";
  public static final String NEGATIVE_OFFSET_ARG_MSG = "Offset argument accepts only non negative values";
  public static final String EXCEPTION_MSG_DELIMITER = ";; ";

  private static final String queryTemplate =
      "SELECT * FROM BILLING_DATA_HOURLY WHERE accountid = '%s' AND clusterid IS NOT NULL LIMIT 1";
  private static final String queryTemplateDaily =
      "SELECT * FROM BILLING_DATA WHERE accountid = '%s' AND clusterid IS NOT NULL LIMIT 1";

  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject private MainConfiguration configuration;
  @Inject protected FeatureFlagService featureFlagService;

  private final LoadingCache<String, Boolean> isClusterDataPresentCache =
      Caffeine.newBuilder().expireAfterAccess(15, TimeUnit.SECONDS).build(this::isAnyClusterDataPresent);

  public String fetchSampleAccountIdIfNoClusterData(@NotNull String accountId) {
    if (featureFlagService.isEnabledReloadCache(FeatureName.CE_SAMPLE_DATA_GENERATION, accountId)) {
      log.info("feature flag CE_SAMPLE_DATA_GENERATION enabled: true");
      if (Boolean.FALSE.equals(isClusterDataPresentCache.get(accountId))) {
        return configuration.getCeSetUpConfig().getSampleAccountId();
      }
    }
    log.info("feature flag CE_SAMPLE_DATA_GENERATION enabled: false");
    return accountId;
  }

  public boolean isAnyClusterDataPresent(String accountId) {
    String clusterQuery = String.format(queryTemplate, accountId);
    String clusterDailyQuery = String.format(queryTemplateDaily, accountId);
    boolean isPresent = getCount(clusterQuery, accountId) != 0 || getCount(clusterDailyQuery, accountId) != 0;
    log.info("Clusterdata for accountId:{} is present {}", accountId, isPresent);
    return isPresent;
  }

  public boolean isSampleClusterDataPresent() {
    String sampleAccountId = configuration.getCeSetUpConfig().getSampleAccountId();
    return isNotEmpty(sampleAccountId) && isAnyClusterDataPresent(sampleAccountId);
  }

  private Integer getCount(String query, String accountId) {
    int count = 0;
    if (timeScaleDBService.isValid()) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(query);
        while (resultSet != null && resultSet.next()) {
          count = 1;
        }
      } catch (SQLException e) {
        log.warn("Failed to execute query in DataFetcherUtils, query=[{}], accountId=[{}], {}", query, accountId, e);
      } finally {
        DBUtils.close(resultSet);
      }
    } else {
      throw new InvalidRequestException("Cannot process request in DataFetcherUtils");
    }
    return count;
  }

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

  public interface Controller<T> {
    void populate(T entity);
  }

  public void setStringFilter(FieldEnd<? extends Query<?>> field, String value) {
    field.equal(value);
  }

  public void setStringFilter(FieldEnd<? extends Query<?>> field, QLStringFilter stringFilter) {
    if (stringFilter == null) {
      throw new WingsException("Filter is null");
    }

    QLStringOperator operator = stringFilter.getOperator();
    if (operator == null) {
      throw new WingsException("String Operator cannot be null");
    }

    String[] stringFilterValues = stringFilter.getValues();

    if (isEmpty(stringFilterValues)) {
      throw new WingsException("Values cannot be empty");
    }

    switch (operator) {
      case IN:
        field.in(Arrays.asList(stringFilterValues));
        break;
      case EQUALS:
        if (stringFilterValues.length > 1) {
          throw new WingsException("Only one value needs to be inputted for operator EQUALS");
        }
        field.equal(stringFilterValues[0]);
        break;
      default:
        throw new WingsException("Unknown String operator " + operator);
    }
  }

  public void setIdFilter(FieldEnd<? extends Query<?>> field, QLIdFilter idFilter) {
    if (idFilter == null) {
      return;
    }

    QLIdOperator operator = idFilter.getOperator();
    if (operator == null) {
      throw new WingsException("Id Operator cannot be null");
    }

    String[] idFilterValues = idFilter.getValues();

    if (isEmpty(idFilterValues)) {
      throw new WingsException("Values cannot be empty");
    }

    switch (operator) {
      case IN:
        field.in(Arrays.asList(idFilterValues));
        break;
      case EQUALS:
        if (idFilterValues.length > 1) {
          throw new WingsException("Only one value needs to be inputted for operator EQUALS");
        }
        field.equal(idFilterValues[0]);
        break;
      case NOT_NULL:
        field.notEqual(null);
        break;
      case NOT_IN:
        field.notIn(Arrays.asList(idFilterValues));
        break;
      default:
        throw new WingsException("Unknown Id operator " + operator);
    }
  }

  public void setEnumFilter(FieldEnd<? extends Query<?>> field, Filter filter) {
    if (filter == null) {
      return;
    }

    QLEnumOperator operator = (QLEnumOperator) filter.getOperator();
    if (operator == null) {
      throw new WingsException("Enum Operator cannot be null");
    }

    if (isEmpty(filter.getValues())) {
      throw new WingsException("Enum values cannot be empty");
    }

    if (!(filter.getValues() instanceof QLEnum[])) {
      throw new WingsException("Incompatible enum filter value");
    }

    QLEnum[] values = (QLEnum[]) filter.getValues();

    List<String> enumValueList = Arrays.stream(values).map(QLEnum::getStringValue).collect(Collectors.toList());
    switch (operator) {
      case IN:
        field.in(enumValueList);
        break;
      case EQUALS:
        if (enumValueList.size() > 1) {
          throw new WingsException("Only one value needs to be inputted for operator EQUALS");
        }
        field.equal(enumValueList.get(0));
        break;
      default:
        throw new WingsException("Unknown Enum operator " + operator);
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

    Number[] numberFilterValues = numberFilter.getValues();

    if (isEmpty(numberFilterValues)) {
      throw new WingsException("Values cannot be empty");
    }

    switch (operator) {
      case EQUALS:
      case NOT_EQUALS:
      case LESS_THAN:
      case LESS_THAN_OR_EQUALS:
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUALS:
        if (numberFilterValues.length > 1) {
          throw new WingsException("Only one value is expected for operator " + operator);
        }
        break;
      default:
        break;
    }

    switch (operator) {
      case EQUALS:
        field.equal(numberFilterValues[0]);
        break;
      case IN:
        field.in(Arrays.asList(numberFilterValues));
        break;
      case NOT_EQUALS:
        field.notEqual(numberFilterValues[0]);
        break;
      case LESS_THAN:
        field.lessThan(numberFilterValues[0]);
        break;
      case LESS_THAN_OR_EQUALS:
        field.lessThanOrEq(numberFilterValues[0]);
        break;
      case GREATER_THAN:
        field.greaterThan(numberFilterValues[0]);
        break;
      case GREATER_THAN_OR_EQUALS:
        field.greaterThanOrEq(numberFilterValues[0]);
        break;
      default:
        throw new WingsException("Unknown Number operator " + operator);
    }
  }

  public void setTimeFilter(FieldEnd<? extends Query<?>> field, QLTimeFilter timeFilter) {
    if (timeFilter == null) {
      throw new WingsException("Filter is null");
    }

    QLTimeOperator operator = timeFilter.getOperator();
    if (operator == null) {
      throw new WingsException("Time operator is null");
    }

    Number[] values = timeFilter.getValues();

    if (isEmpty(values)) {
      throw new WingsException("Values cannot be empty");
    }

    switch (operator) {
      case EQUALS:
      case BEFORE:
      case AFTER:
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
      case BEFORE:
        field.lessThanOrEq(values[0]);
        break;
      case AFTER:
        field.greaterThanOrEq(values[0]);
        break;
      default:
        throw new WingsException("Unknown Time operator " + operator);
    }
  }

  public <M> QLPageInfo populate(QLPageQueryParameters page, Query<M> query, Controller<M> controller) {
    QLPageInfoBuilder builder = QLPageInfo.builder().limit(page.getLimit()).offset(page.getOffset());

    // A full count of all items that match particular filter could be expensive. This is why using has more feature is
    // recommended over obtaining total. To determine if we have more, we fetch 1 more than the requested.
    final FindOptions options = new FindOptions().limit(page.getLimit() + 1).skip(page.getOffset());

    try (HIterator<M> iterator = new HIterator<>(query.fetch(options))) {
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

  public Calendar getDefaultCalendar() {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
  }

  public void ensureNotBlankField(String field, String fieldName) {
    if (StringUtils.isBlank(field)) {
      throw new InvalidRequestException(format("Field: [%s] should not be blank", fieldName));
    }
  }

  public void ensureNotNullField(Object field, String fieldName) {
    if (field == null) {
      throw new InvalidRequestException(format("Field: [%s] is required", fieldName));
    }
  }

  public Principal getTriggeredBy(DataFetchingEnvironment environment) {
    GraphQLContext context = null;

    if (environment.getContext() instanceof GraphQLContext) {
      context = environment.getContext();
    } else if (environment.getContext() instanceof GraphQLContext.Builder) {
      GraphQLContext.Builder builder = environment.getContext();
      context = builder.build();
    } else {
      throw new WingsException("Unknown context");
    }

    String triggeredById = context.get("triggeredById");

    TriggeredByType triggeredByType = context.get("triggeredByType");

    return Principal.builder().triggeredByType(triggeredByType).triggeredById(triggeredById).build();
  }
}
