package software.wings.graphql.datafetcher;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ReportTarget;
import io.harness.logging.ExceptionLogger;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.instance.dashboard.EntitySummary;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLAggregateOperation;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberOperator;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringOperator;
import software.wings.graphql.utils.nameservice.NameResult;
import software.wings.service.impl.instance.DashboardStatisticsServiceImpl.FlatEntitySummaryStats;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public abstract class AbstractStatsDataFetcher<A, F, G, T, S> implements DataFetcher {
  private static final String EXCEPTION_MSG_DELIMITER = ";; ";
  private static final String AGGREGATE_FUNCTION = "aggregateFunction";
  private static final String FILTERS = "filters";
  private static final String GROUP_BY = "groupBy";
  private static final String GROUP_BY_TIME = "groupByTime";
  private static final String SORT_CRITERIA = "sortCriteria";
  private static final String GENERIC_EXCEPTION_MSG = "An error has occurred. Please contact the Harness support team.";

  protected abstract QLData fetch(
      String accountId, A aggregateFunction, List<F> filters, List<G> groupBy, T groupByTime, List<S> sort);

  @Override
  public final Object get(DataFetchingEnvironment dataFetchingEnvironment) {
    Object result;
    try {
      Type[] typeArguments = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
      Class<A> aggregationFuncClass = (Class<A>) typeArguments[0];
      Class<F> filterClass = (Class<F>) typeArguments[1];
      Class<G> groupByClass = (Class<G>) typeArguments[2];
      Class<T> groupByTimeClass = (Class<T>) typeArguments[3];
      Class<S> sortClass = (Class<S>) typeArguments[4];

      final A aggregateFunction = (A) fetchObject(dataFetchingEnvironment, AGGREGATE_FUNCTION, aggregationFuncClass);
      final List<F> filters = (List<F>) fetchObject(dataFetchingEnvironment, FILTERS, filterClass);
      final List<G> groupBy = (List<G>) fetchObject(dataFetchingEnvironment, GROUP_BY, groupByClass);
      final T groupByTime = (T) fetchObject(dataFetchingEnvironment, GROUP_BY_TIME, groupByTimeClass);
      final List<S> sort = (List<S>) fetchObject(dataFetchingEnvironment, SORT_CRITERIA, sortClass);
      result = fetch(AbstractDataFetcher.getAccountId(dataFetchingEnvironment), aggregateFunction, filters, groupBy,
          groupByTime, sort);

    } catch (WingsException ex) {
      throw new WingsException(getCombinedErrorMessages(ex), ex, ex.getReportTargets());
    } catch (Exception ex) {
      throw new WingsException(GENERIC_EXCEPTION_MSG, WingsException.USER_SRE);
    }
    return result;
  }

  private String getCombinedErrorMessages(WingsException ex) {
    List<ResponseMessage> responseMessages = ExceptionLogger.getResponseMessageList(ex, ReportTarget.GRAPHQL_API);
    return responseMessages.stream().map(rm -> rm.getMessage()).collect(Collectors.joining(EXCEPTION_MSG_DELIMITER));
  }

  //  public PermissionAttribute getPermissionAttribute(P parameters) {
  //    return null;
  //  }

  private <O> Object fetchObject(DataFetchingEnvironment dataFetchingEnvironment, String fieldName, Class<O> klass) {
    Object object = dataFetchingEnvironment.getArguments().get(fieldName);
    if (object == null) {
      return null;
    }

    if (object instanceof Collection) {
      Collection returnCollection = Lists.newArrayList();
      Collection collection = (Collection) object;
      collection.forEach(item -> returnCollection.add(convertToObject(item, klass)));
      return returnCollection;
    }
    return convertToObject(object, klass);
  }

  private <O> O convertToObject(Object fromValue, Class<O> klass) {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(fromValue, klass);
  }

  protected String getAccountId(DataFetchingEnvironment environment) {
    GraphQLContext context = environment.getContext();
    String accountId = context.get("accountId");

    if (isEmpty(accountId)) {
      throw new WingsException("accountId is null in the environment");
    }

    return accountId;
  }

  protected void setStringFilter(FieldEnd<? extends Query<?>> field, QLStringFilter stringFilter) {
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

  protected void setNumberFilter(FieldEnd<? extends Query<Instance>> field, QLNumberFilter numberFilter) {
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

  protected String getAggregateFunction(QLAggregateFunction aggregateFunction) {
    QLAggregateOperation aggregateOperation = aggregateFunction.getAggregateOperation();
    switch (aggregateOperation) {
      case MIN:
        return "$min";
      case MAX:
        return "$max";
      case SUM:
        return "$sum";
      case AVERAGE:
        return "$avg";
      default:
        throw new WingsException("Unknown aggregation function" + aggregateOperation);
    }
  }

  protected QLDataPoint getDataPoint(FlatEntitySummaryStats stats, String entityType) {
    QLReference qlReference =
        QLReference.builder().type(entityType).name(stats.getEntityName()).id(stats.getEntityId()).build();
    return QLDataPoint.builder().value(stats.getCount()).key(qlReference).build();
  }

  protected QLDataPoint getDataPoint(FlatEntitySummaryStats stats, String entityType, NameResult nameResult) {
    QLReference qlReference = QLReference.builder()
                                  .type(entityType)
                                  .name(getName(nameResult, stats.getEntityId(), entityType))
                                  .id(stats.getEntityId())
                                  .build();
    return QLDataPoint.builder().value(stats.getCount()).key(qlReference).build();
  }

  protected String getName(NameResult nameResult, String id, String type) {
    if (nameResult.getIdNameMap().containsKey(id)) {
      return nameResult.getIdNameMap().get(id);
    }
    return NameResult.DELETED;
  }

  public abstract String getEntityType();

  @Value
  @Builder
  public static class TwoLevelAggregatedData {
    private EntitySummary firstLevelInfo;
    private EntitySummary secondLevelInfo;
    private long count;
  }
}
