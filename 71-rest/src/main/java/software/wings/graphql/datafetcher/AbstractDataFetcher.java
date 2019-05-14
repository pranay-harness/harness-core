package software.wings.graphql.datafetcher;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
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
import software.wings.security.PermissionAttribute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.validation.constraints.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public abstract class AbstractDataFetcher<T, P> implements DataFetcher {
  public static final String SELECTION_SET_FIELD_NAME = "selectionSet";

  @Inject AuthRuleGraphQL authRuleInstrumentation;

  final Map<String, DataFetcherDirectiveAttributes> parentToContextFieldArgsMap;

  public AbstractDataFetcher() {
    parentToContextFieldArgsMap = Maps.newHashMap();
  }

  public void addDataFetcherDirectiveAttributesForParent(
      String parentTypeName, DataFetcherDirectiveAttributes dataFetcherDirectiveAttributes) {
    parentToContextFieldArgsMap.putIfAbsent(parentTypeName, dataFetcherDirectiveAttributes);
  }

  protected abstract T fetch(P parameters);

  protected CompletionStage<T> fetchWithBatching(P parameters, DataLoader dataLoader) {
    return null;
  }

  @Override
  public final Object get(DataFetchingEnvironment dataFetchingEnvironment) {
    Type[] typeArguments = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
    Class<T> returnClass = (Class<T>) typeArguments[0];
    Class<P> parametersClass = (Class<P>) typeArguments[1];
    final P parameters = fetchParameters(parametersClass, dataFetchingEnvironment);
    authRuleInstrumentation.instrumentDataFetcher(this, dataFetchingEnvironment, parameters, returnClass);
    String parentTypeName = dataFetchingEnvironment.getParentType().getName();
    if (isBatchingRequired(parentTypeName)) {
      String dataFetcherName = getDataFetcherName(parentTypeName);
      return fetchWithBatching(parameters, dataFetchingEnvironment.getDataLoader(dataFetcherName));
    } else {
      return fetch(parameters);
    }
  }

  protected WingsException batchFetchException(Throwable cause) {
    return new WingsException("", cause);
  }

  public PermissionAttribute getPermissionAttribute(P parameters) {
    return null;
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

    Map<String, String> contextFieldArgsMap = getContextFieldArgsMap(dataFetchingEnvironment.getParentType().getName());
    if (contextFieldArgsMap != null) {
      contextFieldArgsMap.forEach(
          (key, value) -> map.put(key, getFieldValue(dataFetchingEnvironment.getSource(), value)));
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
        throw new InvalidRequestException("Limit argument accepts only non negative values");
      }
      if (pageParameters.getOffset() < 0) {
        throw new InvalidRequestException("Offset argument accepts only non negative values");
      }
    }

    return parameters;
  }

  public Object getArgumentValue(DataFetchingEnvironment dataFetchingEnvironment, String argumentName) {
    Object argumentValue = dataFetchingEnvironment.getArgument(argumentName);
    if (argumentValue == null && parentToContextFieldArgsMap != null) {
      Optional<String> parentFieldNameOptional =
          parentToContextFieldArgsMap.values()
              .stream()
              .filter(dataFetcherDirectiveAttributes -> {
                if (dataFetcherDirectiveAttributes == null) {
                  return false;
                }

                Map<String, String> contextFieldArgsMap = dataFetcherDirectiveAttributes.getContextFieldArgsMap();

                if (contextFieldArgsMap == null) {
                  return false;
                }

                String fieldName = contextFieldArgsMap.get(argumentName);
                if (fieldName == null) {
                  return false;
                }

                return true;
              })
              .map(dataFetcherDirectiveAttributes
                  -> dataFetcherDirectiveAttributes.getContextFieldArgsMap().get(argumentName))
              .findFirst();
      if (!parentFieldNameOptional.isPresent()) {
        return null;
      }
      String parentFieldName = parentFieldNameOptional.get();
      argumentValue = getFieldValue(dataFetchingEnvironment.getSource(), parentFieldName);
    }
    return argumentValue;
  }

  private Object getFieldValue(Object obj, String fieldName) {
    try {
      return PropertyUtils.getProperty(obj, fieldName);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
      throw new InvalidRequestException(String.format("Failed to obtain the value for field %s", fieldName), exception);
    }
  }

  protected String getAccountId(DataFetchingEnvironment environment) {
    GraphQLContext context = environment.getContext();
    String accountId = context.get("accountId");

    if (isEmpty(accountId)) {
      throw new WingsException("accountId is null in the environment");
    }

    return accountId;
  }

  private Map<String, String> getContextFieldArgsMap(String parentTypeName) {
    DataFetcherDirectiveAttributes dataFetcherDirectiveAttributes = parentToContextFieldArgsMap.get(parentTypeName);
    Map<String, String> contextFieldArgsMap = null;
    if (dataFetcherDirectiveAttributes != null) {
      contextFieldArgsMap = dataFetcherDirectiveAttributes.getContextFieldArgsMap();
    }
    return contextFieldArgsMap;
  }

  private String getDataFetcherName(@NotNull String parentTypeName) {
    return parentToContextFieldArgsMap.get(parentTypeName).getDataFetcherName();
  }

  private boolean isBatchingRequired(@NotNull String parentTypeName) {
    return parentToContextFieldArgsMap.get(parentTypeName).getUseBatch();
  }
}
