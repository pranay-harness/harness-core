package software.wings.audit;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EntityAuditRecord {
  public static final String OPERATION_TYPE_CREATE = "CREATE";
  public static final String OPERATION_TYPE_UPDATE = "UPDATE";
  public static final String OPERATION_TYPE_DELETE = "DELETE";

  // Details of the entity being modified
  private String entityId;
  private String entityType;
  private String entityName;
  private String operationType;
  private String entityOldYamlRecordId;
  private String entityNewYamlRecordId;

  // Details of the affected application.
  // May be NULL for account level entities.
  // The application column is always there.
  // Hence maintained separately.
  private String appId;
  private String appName;

  // Details of the affected resource.
  // Mostly, this could be Service / Environment.
  // Added separately to make indexing and
  // UI aggregation easier.
  private String affectedResourceId;
  private String affectedResourceName;
  private String affectedResourceType;
  private String affectedResourceOperation;
}