package software.wings.graphql.datafetcher;

import static software.wings.graphql.datafetcher.AbstractDataFetcher.SELECTION_SET_FIELD_NAME;

import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ReportTarget;
import io.harness.logging.ExceptionLogger;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;
import software.wings.graphql.schema.type.QLContextedObject;
import software.wings.graphql.schema.type.aggregation.QLData;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public abstract class AbstractStatsDataFetcher<A, F, G, T> implements DataFetcher {
  private static final String EXCEPTION_MSG_DELIMITER = ";; ";
  private static final String GENERIC_EXCEPTION_MSG = "An error has occurred. Please contact the Harness support team.";

  protected abstract QLData fetch(A aggregateFunction, List<F> filters, List<G> groupBy, T groupByTime);

  @Override
  public final Object get(DataFetchingEnvironment dataFetchingEnvironment) {
    Object result;
    try {
      //      final Map<String, Object> aggregatedMap = (Map) fetchObject(dataFetchingEnvironment, "aggregateFunction");
      Type[] typeArguments = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
      Class<A> aggregationClass = (Class<A>) typeArguments[0];
      Class<T> timeGroupByClass = (Class<T>) typeArguments[3];

      final A aggregateFunction = (A) fetchObject(aggregationClass, dataFetchingEnvironment);
      final List<F> filters = (List<F>) fetchObject(dataFetchingEnvironment, "filters");
      final List<G> groupBy = (List<G>) fetchObject(dataFetchingEnvironment, "groupBy");
      final T groupByTime = (T) fetchObject(timeGroupByClass, dataFetchingEnvironment);
      result = fetch(aggregateFunction, filters, groupBy, groupByTime);

    } catch (WingsException ex) {
      throw new WingsException(getCombinedErrorMessages(ex), ex, ex.getReportTargets());
    } catch (Exception ex) {
      throw new WingsException(GENERIC_EXCEPTION_MSG, WingsException.USER_SRE);
    }
    return result;
  }

  private static final Objenesis objenesis = new ObjenesisStd(true);
  private Object fetchObject(Class clazz, DataFetchingEnvironment dataFetchingEnvironment) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration()
        .setMatchingStrategy(MatchingStrategies.STANDARD)
        .setFieldMatchingEnabled(true)
        .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE);

    Object parameters = objenesis.newInstance(clazz);
    Map<String, Object> map = new HashMap<>(dataFetchingEnvironment.getArguments());
    if (dataFetchingEnvironment.getSource() instanceof QLContextedObject) {
      map.putAll(((QLContextedObject) dataFetchingEnvironment.getSource()).getContext());
    }

    modelMapper.map(map, parameters);
    if (FieldUtils.getField(clazz, SELECTION_SET_FIELD_NAME, true) != null) {
      try {
        FieldUtils.writeField(parameters, SELECTION_SET_FIELD_NAME, dataFetchingEnvironment.getSelectionSet(), true);
      } catch (IllegalAccessException exception) {
        logger.error("This should not happen", exception);
      }
    }

    return parameters;
  }

  private String getCombinedErrorMessages(WingsException ex) {
    List<ResponseMessage> responseMessages = ExceptionLogger.getResponseMessageList(ex, ReportTarget.GRAPHQL_API);
    return responseMessages.stream().map(rm -> rm.getMessage()).collect(Collectors.joining(EXCEPTION_MSG_DELIMITER));
  }

  //  public PermissionAttribute getPermissionAttribute(P parameters) {
  //    return null;
  //  }

  private Object fetchObject(DataFetchingEnvironment dataFetchingEnvironment, String fieldName) {
    return dataFetchingEnvironment.getArguments().get(fieldName);
  }

  protected String getAccountId(DataFetchingEnvironment environment) {
    GraphQLContext context = environment.getContext();
    String accountId = context.get("accountId");

    if (EmptyPredicate.isEmpty(accountId)) {
      throw new WingsException("accountId is null in the environment");
    }

    return accountId;
  }
}
