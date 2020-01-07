package software.wings.graphql.datafetcher;

import static io.harness.exception.WingsException.ReportTarget.GRAPHQL_API;
import static io.harness.exception.WingsException.USER_SRE;
import static java.lang.String.format;
import static software.wings.graphql.datafetcher.DataFetcherUtils.EXCEPTION_MSG_DELIMITER;
import static software.wings.graphql.datafetcher.DataFetcherUtils.GENERIC_EXCEPTION_MSG;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetchingEnvironment;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseMutatorDataFetcher<P, R> extends BaseDataFetcher {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private Class<P> parameterClass;
  private Class<R> resultClass;

  public BaseMutatorDataFetcher(Class<P> parameterClass, Class<R> resultClass) {
    this.parameterClass = parameterClass;
    this.resultClass = resultClass;
  }

  protected abstract R mutateAndFetch(P parameter, MutationContext mutationContext);

  @Override
  public R get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
    try {
      final P parameters = fetchParameters(parameterClass, dataFetchingEnvironment);
      authRuleInstrumentation.instrumentDataFetcher(this, dataFetchingEnvironment, resultClass);
      final MutationContext mutationContext = MutationContext.builder()
                                                  .accountId(utils.getAccountId(dataFetchingEnvironment))
                                                  .dataFetchingEnvironment(dataFetchingEnvironment)
                                                  .build();
      final R result = mutateAndFetch(parameters, mutationContext);
      handlePostMutation(mutationContext, parameters, result);
      return result;
    } catch (WingsException ex) {
      throw new InvalidRequestException(getCombinedErrorMessages(ex), ex, ex.getReportTargets());
    } catch (Exception ex) {
      throw new InvalidRequestException(GENERIC_EXCEPTION_MSG, ex, USER_SRE);
    }
  }

  private P fetchParameters(Class<P> parameterClass, DataFetchingEnvironment dataFetchingEnvironment) {
    return convertToObject(dataFetchingEnvironment.getArguments(), parameterClass);
  }
  private <M> M convertToObject(Object fromValue, Class<M> klass) {
    return MAPPER.convertValue(fromValue, klass);
  }

  private String getCombinedErrorMessages(WingsException ex) {
    List<ResponseMessage> responseMessages = ExceptionLogger.getResponseMessageList(ex, GRAPHQL_API);
    return responseMessages.stream()
        .map(ResponseMessage::getMessage)
        .collect(Collectors.joining(EXCEPTION_MSG_DELIMITER));
  }

  private void handlePostMutation(MutationContext mutationContext, P parameter, R mutationResult) {
    final DataFetchingEnvironment dataFetchingEnvironment = mutationContext.getDataFetchingEnvironment();
    final String accountId = mutationContext.getAccountId();
    try {
      // this should not fail the overall mutation, so caught the exception
      authRuleInstrumentation.handlePostMutation(mutationContext, parameter, mutationResult);
    } catch (Exception e) {
      logger.error(format("Cache eviction failed for mutation api [%s] for accountId [%s]",
                       dataFetchingEnvironment.getField().getName(), accountId),
          e);
    }
  }
}
