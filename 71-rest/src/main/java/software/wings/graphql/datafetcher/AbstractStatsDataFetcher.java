package software.wings.graphql.datafetcher;

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
import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;
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
      final A aggregateFunction = (A) fetchObject(dataFetchingEnvironment, "aggregateFunction");
      final List<F> filters = (List<F>) fetchObject(dataFetchingEnvironment, "filters");
      final List<G> groupBy = (List<G>) fetchObject(dataFetchingEnvironment, "groupBy");
      final T groupByTime = (T) fetchObject(dataFetchingEnvironment, "groupByTime");
      result = fetch(aggregateFunction, filters, groupBy, groupByTime);

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
