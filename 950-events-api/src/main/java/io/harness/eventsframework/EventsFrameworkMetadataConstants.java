package io.harness.eventsframework;

public final class EventsFrameworkMetadataConstants {
  public static final String ENTITY_TYPE = "entityType";

  public static final String ACTION = "action";
  public static final String CREATE_ACTION = "create";
  public static final String RESTORE_ACTION = "restore";
  public static final String UPDATE_ACTION = "update";
  public static final String DELETE_ACTION = "delete";
  public static final String FLUSH_CREATE_ACTION = "flushCreate";

  public static final String PROJECT_ENTITY = "project";
  public static final String ORGANIZATION_ENTITY = "organization";
  public static final String CONNECTOR_ENTITY = "connector";

  // deprecated, use setupusage and entityActivity channel.
  public static final String SETUP_USAGE_ENTITY = "setupUsage";
  public static final String ACCOUNT_ENTITY = "account";

  public static final String REFERRED_ENTITY_TYPE = "referredEntityType";
}
