package software.wings.audit;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EntityAuditRecord {
  public static final String OPERATION_TYPE_CREATE = "CREATE";
  public static final String OPERATION_TYPE_UPDATE = "UPDATE";
  public static final String OPERATION_TYPE_DELETE = "DELETE";

  private String entityId;
  private String entityType;
  private String entityName;
  private String operationType;
  private String entityOldYamlRecordId;
  private String entityNewYamlRecordId;
}