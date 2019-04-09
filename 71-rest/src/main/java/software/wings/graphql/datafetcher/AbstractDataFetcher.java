package software.wings.graphql.datafetcher;

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

  protected boolean isAuthorizedToView(String appId, PermissionAttribute permissionAttribute, String entityId) {
    List<PermissionAttribute> permissionAttributeList = asList(permissionAttribute);
    return authHandler.authorize(permissionAttributeList, asList(appId), entityId);
  }

  protected int getPageLimit(@NotNull DataFetchingEnvironment environment) {
    Integer limit = environment.getArgument(GraphQLConstants.PAGE_LIMIT);
    if (limit == null || limit > MAX_PAGE_SIZE) {
      limit = GraphQLConstants.MAX_PAGE_SIZE;
    }
    return limit;
  }

  protected int getPageOffset(@NotNull DataFetchingEnvironment environment) {
    Integer offset = environment.getArgument(GraphQLConstants.PAGE_OFFSET);
    if (offset == null) {
      offset = GraphQLConstants.ZERO_OFFSET;
    }
    return offset;
  }

  protected void addInvalidInputInfo(BaseInfo baseInfo, String entityName) {
    String invalidInputMsg = String.format(EMPTY_OR_NULL_INPUT_FIELD, entityName);
    baseInfo.setDebugInfo(invalidInputMsg);
  }

  protected void addNoRecordFoundInfo(BaseInfo baseInfo, String messageString, Object... values) {
    String noRecordsFoundMsg = String.format(messageString, values);
    baseInfo.setDebugInfo(noRecordsFoundMsg);
  }

  protected void throwNotAuthorizedException(String entityName, String id, String appId) {
    String errorMsg = String.format(USER_NOT_AUTHORIZED_TO_VIEW_ENTITY, entityName, id, appId);
    throw new WingsException(errorMsg, WingsException.USER);
  }

  /**
   * TODO This method implementation has to be changed later on
   * @return
   */
  protected Account getAccount() {
    User currentUser = UserThreadLocal.get();
    return currentUser.getAccounts().get(0);
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
    } catch (NoSuchFieldException e) {
      log.warn("NoSuchFieldException occured while fetching value for field {}", fieldName);
    } catch (IllegalAccessException e) {
      log.warn("IllegalAccessException occured while fetching value for field {}", fieldName);
    } catch (IntrospectionException e) {
      log.warn("IntrospectionException occured while fetching value for field {}", fieldName);
    } catch (InvocationTargetException e) {
      log.warn("InvocationTargetException occured while fetching value for field {}", fieldName);
    }

    return fieldValue;
  }
}
