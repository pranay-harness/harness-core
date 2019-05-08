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
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserThreadLocal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public abstract class AbstractDataFetcher<T, P> implements DataFetcher {
  public static final String SELECTION_SET_FIELD_NAME = "selectionSet";

  @Inject AuthRuleGraphQL authRuleInstrumentation;

  final Map<String, Map<String, String>> parentToContextFieldArgsMap;

  public AbstractDataFetcher() {
    parentToContextFieldArgsMap = Maps.newHashMap();
  }

  public void addParentContextFieldArgMapFor(String parentTypeName, Map<String, String> contextFieldArgsMap) {
    parentToContextFieldArgsMap.putIfAbsent(parentTypeName, contextFieldArgsMap);
  }

  protected abstract T fetch(P parameters);

  @Override
  public final Object get(DataFetchingEnvironment dataFetchingEnvironment) {
    Class<P> parametersClass =
        (Class<P>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    final P parameters = fetchParameters(parametersClass, dataFetchingEnvironment);
    Class<T> returnClass =
        (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    authRuleInstrumentation.instrumentDataFetcher(this, dataFetchingEnvironment, parameters, returnClass);
    return fetch(parameters);
  }

  protected WingsException batchFetchException(Throwable cause) {
    return new WingsException("", cause);
  }

  public PermissionAttribute getPermissionAttribute(P parameters) {
    return null;
  }

  /**
   * TODO This method implementation has to be changed later on
   * @return
   */
  protected Account getAccount() {
    User currentUser = UserThreadLocal.get();
    return currentUser.getAccounts().get(0);
  }

  private static final Objenesis objenesis = new ObjenesisStd(true);

  protected P fetchParameters(Class<P> clazz, DataFetchingEnvironment dataFetchingEnvironment) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration()
        .setMatchingStrategy(MatchingStrategies.STANDARD)
        .setFieldMatchingEnabled(true)
        .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE);

    P parameters = objenesis.newInstance(clazz);
    Map<String, Object> map = new HashMap<>(dataFetchingEnvironment.getArguments());
    Map<String, String> contextFieldArgsMap =
        parentToContextFieldArgsMap.get(dataFetchingEnvironment.getParentType().getName());
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
      Optional<String> parentFieldNameOptional = parentToContextFieldArgsMap.values()
                                                     .stream()
                                                     .filter(contextFieldArgsMap -> {
                                                       if (contextFieldArgsMap == null) {
                                                         return false;
                                                       }
                                                       String fieldName = contextFieldArgsMap.get(argumentName);
                                                       if (fieldName == null) {
                                                         return false;
                                                       }

                                                       return true;
                                                     })
                                                     .map(contextFieldArgsMap -> contextFieldArgsMap.get(argumentName))
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
}
