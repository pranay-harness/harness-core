package software.wings.audit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EntityAuditRecord {
  // Details of the entity being modified
  private String entityId;
  private String entityType;
  private String entityName;
  private String operationType;
  private String entityOldYamlRecordId;
  private String entityNewYamlRecordId;
  private String yamlPath;
  private String yamlError;
  private boolean failure;

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