package software.wings.graphql.datafetcher;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
import software.wings.service.impl.security.auth.AuthHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public abstract class AbstractDataFetcher<T, P> implements DataFetcher {
  public static final String SELECTION_SET_FIELD_NAME = "selectionSet";

  @NotNull final AuthHandler authHandler;
  @Setter Map<String, String> contextFieldArgsMap;
  @Getter @Setter String batchedDataLoaderName;

  protected abstract T fetch(P parameters);

  @Override
  public final Object get(DataFetchingEnvironment dataFetchingEnvironment) {
    Class<P> parametersClass =
        (Class<P>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    final P parameters = fetchParameters(parametersClass, dataFetchingEnvironment);
    return fetch(parameters);
  }

  protected boolean isAuthorizedToView(String appId, PermissionAttribute permissionAttribute, String entityId) {
    List<PermissionAttribute> permissionAttributeList = asList(permissionAttribute);
    return authHandler.authorize(permissionAttributeList, asList(appId), entityId);
  }

  protected WingsException batchFetchException(Throwable cause) {
    return new WingsException("", cause);
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

  private Object getFieldValue(Object obj, String fieldName) {
    try {
      return PropertyUtils.getProperty(obj, fieldName);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
      throw new InvalidRequestException(format("Failed to obtain the value for field %s", fieldName), exception);
    }
  }
}
