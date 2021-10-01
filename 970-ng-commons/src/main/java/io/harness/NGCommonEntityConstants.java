package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class NGCommonEntityConstants {
  public static final String ACCOUNT_KEY = "accountIdentifier";
  public static final String ORG_KEY = "orgIdentifier";
  public static final String PROJECT_KEY = "projectIdentifier";
  public static final String VERSION_LABEL_KEY = "versionLabel";
  public static final String SERVICE_KEY = "serviceId";
  public static final String ENVIRONMENT_KEY = "envId";
  public static final String BUILDS_KEY = "buildIds";
  public static final String TIMESTAMP = "timestamp";
  public static final String IDENTIFIER_KEY = "identifier";
  public static final String GROUP_IDENTIFIER_KEY = "groupIdentifier";
  public static final String GROUP_IDENTIFIERS_KEY = "groupIdentifiers";
  public static final String NAME_KEY = "name";
  public static final String TAGS_KEY = "tags";
  public static final String DESCRIPTION_KEY = "description";
  public static final String DELETED_KEY = "deleted";
  public static final String PIPELINE_KEY = "pipelineIdentifier";
  public static final String INPUT_SET_IDENTIFIER_KEY = "inputSetIdentifier";
  public static final String ENVIRONMENT_IDENTIFIER_KEY = "environmentIdentifier";
  public static final String STATUS = "status";
  public static final String MONGODB_ID = "_id";
  public static final String CONNECTOR_IDENTIFIER_KEY = "connectorIdentifier";
  public static final String REPO_URL = "repoURL";
  public static final String PLAN_KEY = "planExecutionId";
  public static final String TYPE_KEY = "type";
  public static final String TOKEN_KEY = "tokenId";
  public static final String REFERRED_ENTITY_TYPE = "referredEntityType";
  public static final String REFERRED_BY_ENTITY_TYPE = "referredByEntityType";
  public static final String MODULE_TYPE = "moduleType";
  public static final String ENTITY_TYPE = "entityType";
  public static final String SEARCH_TERM = "searchTerm";
  public static final String USER_ID = "userId";
  public static final String PAGE = "page";
  public static final String SIZE = "size";
  public static final String MASK_SECRETS = "maskSecrets";
  public static final String CONNECTOR_IDENTIFIER_REF = "connectorIdentifierRef";
  public static final String CONNECTOR_TYPE = "connectorType";
  public static final String PERPETUAL_TASK_ID = "perpetualTaskId";
  public static final String RETRY_STAGES = "retryStages";
  public static final String RUN_ALL_STAGES = "runAllStages";
  public static final Integer MAX_PAGE_SIZE = 10000;

  // TODO(UTSAV): Move to 340-ce-nextgen
  public static final String IS_EVENTS_ENABLED = "eventsEnabled";
  public static final String IS_OPTIMIZATION_ENABLED = "optimizationEnabled";
  public static final String IS_CUR_ENABLED = "curEnabled";
}
