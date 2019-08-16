package software.wings.graphql.datafetcher;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.ReportTarget.GRAPHQL_API;
import static io.harness.exception.WingsException.USER_SRE;
import static software.wings.graphql.datafetcher.DataFetcherUtils.GENERIC_EXCEPTION_MSG;
import static software.wings.graphql.datafetcher.DataFetcherUtils.NEGATIVE_LIMIT_ARG_MSG;
import static software.wings.graphql.datafetcher.DataFetcherUtils.NEGATIVE_OFFSET_ARG_MSG;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.dataloader.DataLoader;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;
import software.wings.graphql.directive.DataFetcherDirective.DataFetcherDirectiveAttributes;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLContextedObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public abstract class AbstractDataFetcher<T, P> extends BaseDataFetcher {
  public static final String SELECTION_SET_FIELD_NAME = "selectionSet";

  public void addDataFetcherDirectiveAttributesForParent(
      String parentTypeName, DataFetcherDirectiveAttributes dataFetcherDirectiveAttributes) {
    parentToContextFieldArgsMap.putIfAbsent(parentTypeName, dataFetcherDirectiveAttributes);
  }

  protected abstract T fetch(P parameters, String accountId);

  protected CompletionStage<T> fetchWithBatching(P parameters, DataLoader dataLoader) {
    return null;
  }

  @Override
  public final Object get(DataFetchingEnvironment dataFetchingEnvironment) {
    Type[] typeArguments = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
    Class<T> returnClass = (Class<T>) typeArguments[0];
    Class<P> parametersClass = (Class<P>) typeArguments[1];
    Object result;
    try {
      final P parameters = fetchParameters(parametersClass, dataFetchingEnvironment);
      authRuleInstrumentation.instrumentDataFetcher(this, dataFetchingEnvironment, returnClass);
      String parentTypeName = dataFetchingEnvironment.getParentType().getName();
      if (isBatchingRequired(parentTypeName)) {
        String dataFetcherName = getDataFetcherName(parentTypeName);
        result = fetchWithBatching(parameters, dataFetchingEnvironment.getDataLoader(dataFetcherName));
      } else {
        result = fetch(parameters, getAccountId(dataFetchingEnvironment));
      }
    } catch (WingsException ex) {
      throw new WingsException(getCombinedErrorMessages(ex), ex, ex.getReportTargets());
    } catch (Exception ex) {
      throw new WingsException(GENERIC_EXCEPTION_MSG, USER_SRE);
    }
    return result;
  }

  private String getAccountId(DataFetchingEnvironment dataFetchingEnvironment) {
    Object contextObj = dataFetchingEnvironment.getContext();

    if (!(contextObj instanceof GraphQLContext)) {
      throw new WingsException("Context not a graphqlContext");
    }

    GraphQLContext context = (GraphQLContext) contextObj;
    String accountId = context.get("accountId");
    if (isEmpty(accountId)) {
      throw new WingsException("Cannot extract accountId from environment");
    }

    return accountId;
  }

  private String getCombinedErrorMessages(WingsException ex) {
    List<ResponseMessage> responseMessages = ExceptionLogger.getResponseMessageList(ex, GRAPHQL_API);
    return responseMessages.stream()
        .map(rm -> rm.getMessage())
        .collect(Collectors.joining(DataFetcherUtils.EXCEPTION_MSG_DELIMITER));
  }

  private static final Objenesis objenesis = new ObjenesisStd(true);

  private P fetchParameters(Class<P> clazz, DataFetchingEnvironment dataFetchingEnvironment) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration()
        .setMatchingStrategy(MatchingStrategies.STANDARD)
        .setFieldMatchingEnabled(true)
        .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE);

    P parameters = objenesis.newInstance(clazz);
    Map<String, Object> map = new HashMap<>(dataFetchingEnvironment.getArguments());
    if (dataFetchingEnvironment.getSource() instanceof QLContextedObject) {
      map.putAll(((QLContextedObject) dataFetchingEnvironment.getSource()).getContext());
    }

    Map<String, String> contextFieldArgsMap =
        utils.getContextFieldArgsMap(parentToContextFieldArgsMap, dataFetchingEnvironment.getParentType().getName());
    if (contextFieldArgsMap != null) {
      contextFieldArgsMap.forEach(
          (key, value) -> map.put(key, utils.getFieldValue(dataFetchingEnvironment.getSource(), value)));
    }
    modelMapper.map(map, parameters);
    if (FieldUtils.getField(clazz, SELECTION_SET_FIELD_NAME, true) != null) {
      try {
        FieldUtils.writeField(parameters, SELECTION_SET_FIELD_NAME, dataFetchingEnvironment.getSelectionSet(), true);
      } catch (IllegalAccessException exception) {
        logger.error("This should not happen", exception);
      }
    }

    if (parameters instanceof QLPageQueryParameters) {
      QLPageQueryParameters pageParameters = (QLPageQueryParameters) parameters;
      if (pageParameters.getLimit() < 0) {
        throw new InvalidRequestException(NEGATIVE_LIMIT_ARG_MSG);
      }
      if (pageParameters.getOffset() < 0) {
        throw new InvalidRequestException(NEGATIVE_OFFSET_ARG_MSG);
      }
    }

    return parameters;
  }

  private String getDataFetcherName(@NotNull String parentTypeName) {
    return parentToContextFieldArgsMap.get(parentTypeName).getDataFetcherName();
  }

  private boolean isBatchingRequired(@NotNull String parentTypeName) {
    return parentToContextFieldArgsMap.get(parentTypeName).getUseBatch();
  }
}
