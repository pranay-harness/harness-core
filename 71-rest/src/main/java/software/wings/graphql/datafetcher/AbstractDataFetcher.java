package software.wings.graphql.datafetcher;

import static java.util.Arrays.asList;
import static software.wings.graphql.utils.GraphQLConstants.EMPTY_OR_NULL_INPUT_FIELD;
import static software.wings.graphql.utils.GraphQLConstants.MAX_PAGE_SIZE;
import static software.wings.graphql.utils.GraphQLConstants.USER_NOT_AUTHORIZED_TO_VIEW_ENTITY;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.WingsException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.schema.type.DebugInfo;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.security.PermissionAttribute;
import software.wings.service.impl.security.auth.AuthHandler;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public abstract class AbstractDataFetcher {
  AuthHandler authHandler;

  protected boolean isAuthorizedToView(String appId, PermissionAttribute permissionAttribute, String entityId) {
    List<PermissionAttribute> permissionAttributeList = asList(permissionAttribute);
    return authHandler.authorize(permissionAttributeList, asList(appId), entityId);
  }

  protected abstract Map<String, DataFetcher<?>> getOperationToDataFetcherMap();

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

  protected void addInvalidInputInfo(DebugInfo debugInfo, String entityName) {
    String invalidInputMsg = String.format(EMPTY_OR_NULL_INPUT_FIELD, entityName);
    debugInfo.setDebugInfo(invalidInputMsg);
  }

  protected void addNoRecordFoundInfo(DebugInfo debugInfo, String messageString, Object... values) {
    String noRecordsFoundMsg = String.format(messageString, values);
    debugInfo.setDebugInfo(noRecordsFoundMsg);
  }

  protected void throwNotAuthorizedException(String entityName, String id, String appId) {
    String errorMsg = String.format(USER_NOT_AUTHORIZED_TO_VIEW_ENTITY, entityName, id, appId);
    throw new WingsException(errorMsg, WingsException.USER);
  }
}
