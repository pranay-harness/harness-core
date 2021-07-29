package software.wings.service.impl.instance;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;

@Value
@Builder
@FieldNameConstants(innerTypeName = "ServiceInfoResponseSummaryKeys")
public class ServiceInfoResponseSummary {
  String lastArtifactBuildNum;
  String lastWorkflowExecutionId;
  String lastWorkflowExecutionName;
  String infraMappingId;
  String infraMappingName;

  @UtilityClass
  public static final class ServiceInfoResponseSummaryKeys {
    public static final String lastArtifactBuildNum = "lastArtifactBuildNum";
    public static final String lastWorkflowExecutionId = "lastWorkflowExecutionId";
    public static final String lastWorkflowExecutionName = "lastWorkflowExecutionName";
    public static final String infraMappingId = "infraMappingId";
    public static final String infraMappingName = "infraMappingName";
  }
}
