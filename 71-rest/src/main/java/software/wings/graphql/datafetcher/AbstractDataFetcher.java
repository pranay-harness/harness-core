package software.wings.graphql.datafetcher;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.graphql.utils.GraphQLConstants.EMPTY_OR_NULL_INPUT_FIELD;
import static software.wings.graphql.utils.GraphQLConstants.MAX_PAGE_SIZE;
import static software.wings.graphql.utils.GraphQLConstants.USER_NOT_AUTHORIZED_TO_VIEW_ENTITY;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.WingsException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.graphql.schema.type.BaseInfo;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.security.auth.AuthHandler;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public abstract class AbstractDataFetcher<T> implements DataFetcher {
  @NotNull final AuthHandler authHandler;
  @Setter Map<String, String> contextFieldArgsMap;
  @Getter @Setter String batchedDataLoaderName;

  protected abstract T fetch(DataFetchingEnvironment dataFetchingEnvironment);

  @Override
  public final Object get(DataFetchingEnvironment dataFetchingEnvironment) {
    return fetch(dataFetchingEnvironment);
  }

  protected boolean isAuthorizedToView(String appId, PermissionAttribute permissionAttribute, String entityId) {
    List<PermissionAttribute> permissionAttributeList = asList(permissionAttribute);
    return authHandler.authorize(permissionAttributeList, asList(appId), entityId);
  }

  protected int getPageLimit(@NotNull DataFetchingEnvironment environment) {
    Integer limit = environment.getArgument(GraphQLConstants.PAGE_LIMIT_ARG);
    if (limit == null || limit > MAX_PAGE_SIZE) {
      limit = GraphQLConstants.MAX_PAGE_SIZE;
    }
    return limit;
  }

  protected int getPageOffset(@NotNull DataFetchingEnvironment environment) {
    Integer offset = environment.getArgument(GraphQLConstants.PAGE_OFFSET_ARG);
    if (offset == null) {
      offset = GraphQLConstants.ZERO_OFFSET;
    }
    return offset;
  }

  protected void addInvalidInputInfo(BaseInfo baseInfo, String entityName) {
    String invalidInputMsg = format(EMPTY_OR_NULL_INPUT_FIELD, entityName);
    baseInfo.setDebugInfo(invalidInputMsg);
  }

  protected void addNoRecordFoundInfo(BaseInfo baseInfo, String messageString, Object... values) {
    String noRecordsFoundMsg = format(messageString, values);
    baseInfo.setDebugInfo(noRecordsFoundMsg);
  }

  protected WingsException notAuthorizedException(String entityName, String id, String appId) {
    String errorMsg = format(USER_NOT_AUTHORIZED_TO_VIEW_ENTITY, entityName, id, appId);
    throw new WingsException(errorMsg, WingsException.USER);
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

  protected <P> P fetchParameters(Class<P> clazz, DataFetchingEnvironment dataFetchingEnvironment) {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration()
        .setMatchingStrategy(MatchingStrategies.STANDARD)
        .setFieldMatchingEnabled(true)
        .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE);

    P parameters = objenesis.newInstance(clazz);
    modelMapper.map(dataFetchingEnvironment.getArguments(), parameters);
    try {
      FieldUtils.writeField(parameters, "selectionSet", dataFetchingEnvironment.getSelectionSet(), true);
    } catch (IllegalAccessException exception) {
      logger.error("This should not happen", exception);
    }

    return parameters;
  }

  protected Object getArgumentValue(DataFetchingEnvironment dataFetchingEnvironment, String argumentName) {
    Object argumentValue = dataFetchingEnvironment.getArgument(argumentName);
    if (argumentValue == null && contextFieldArgsMap != null) {
      String parentFieldName = contextFieldArgsMap.get(argumentName);
      argumentValue = getFieldValue(dataFetchingEnvironment.getSource(), parentFieldName);
    }
    return argumentValue;
  }

  private Object getFieldValue(Object obj, String fieldName) {
    Object fieldValue = null;
    Class<?> clazz = obj.getClass();
    try {
      Field field = clazz.getDeclaredField(fieldName);
      if (field != null) {
        PropertyDescriptor pd = new PropertyDescriptor(field.getName(), obj.getClass());
        fieldValue = pd.getReadMethod().invoke(obj);
      }
    } catch (
        NoSuchFieldException | IllegalAccessException | IntrospectionException | InvocationTargetException exception) {
      logger.warn(format("NoSuchFieldException occurred while fetching value for field %s", fieldName), exception);
    }

    return fieldValue;
  }
}
