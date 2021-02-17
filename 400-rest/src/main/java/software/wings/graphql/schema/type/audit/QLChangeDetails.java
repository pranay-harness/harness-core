package software.wings.graphql.schema.type.audit;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLChangeDetails implements QLObject {
  private String resourceId;
  private String resourceType;
  private String resourceName;
  private String operationType;
  private Boolean failure;
  private String appId;
  private String appName;
  private String parentResourceId;
  private String parentResourceName;
  private String parentResourceType;
  private String parentResourceOperation;
  private Long createdAt;
}
