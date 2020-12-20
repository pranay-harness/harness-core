package io.harness;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EntityCRUDEventsConstants {
  public static final String ENTITY_CRUD = "entity_crud";
  public static final String ENTITY_TYPE_METADATA = "entityType";

  public static final String ACTION_METADATA = "action";
  public static final String CREATE_ACTION = "create";
  public static final String UPDATE_ACTION = "update";
  public static final String DELETE_ACTION = "delete";

  public static final String PROJECT_ENTITY = "project";
  public static final String ORGANIZATION_ENTITY = "organization";
  public static final String CONNECTOR_ENTITY = "connector";
  public static final String SETUP_USAGE_ENTITY = "setupUsage";
  public static final String ACTIVITY_ENTITY = "entityActivity";
  public static final String ACCOUNT_ENTITY = "account";
}
