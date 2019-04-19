package software.wings.graphql.schema.type;

import io.harness.beans.WorkflowType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import software.wings.beans.Service;

import java.util.List;

/**
 * The reason I am defining this class as QLWorkflow
 * is because we already have an enum named as WorkflowType.
 * If required will change the name to something more meaningful.
 */
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLWorkflow implements QLObject, BaseInfo {
  String id;
  String name;
  String description;
  WorkflowType workflowType;
  boolean templatized;
  List<Service> services;
  String envId;
  String appId;
  String debugInfo;
}
