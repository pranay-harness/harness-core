package io.harness.delegate.beans.ci.vm;

import io.harness.delegate.beans.ci.CIInitializeTaskParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.ConnectorTaskParams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIVmInitializeTaskParams extends ConnectorTaskParams implements CIInitializeTaskParams {
  @NotNull private String poolID;
  @NotNull private String workingDir;

  @NotNull private String logStreamUrl;
  @NotNull private String logSvcToken;
  @NotNull private boolean logSvcIndirectUpload;

  @NotNull private String tiUrl;
  @NotNull private String tiSvcToken;

  @NotNull private String accountID;
  @NotNull private String orgID;
  @NotNull private String projectID;
  @NotNull private String pipelineID;
  @NotNull private String stageID;
  @NotNull private String buildID;

  Map<String, String> environment;
  private ConnectorDetails gitConnector;

  private String stageRuntimeId;
  @Builder.Default private static final Type type = Type.VM;

  @Override
  public Type getType() {
    return type;
  }
}
