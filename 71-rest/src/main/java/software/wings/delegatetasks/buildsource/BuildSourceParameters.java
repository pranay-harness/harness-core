package software.wings.delegatetasks.buildsource;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class BuildSourceParameters implements TaskParameters, ExecutionCapabilityDemander {
  public enum BuildSourceRequestType { GET_BUILDS, GET_LAST_SUCCESSFUL_BUILD }

  @NotNull private BuildSourceRequestType buildSourceRequestType;
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @NotNull private SettingValue settingValue;
  @NotNull private ArtifactStreamAttributes artifactStreamAttributes;
  @NotNull private List<EncryptedDataDetail> encryptedDataDetails;
  @NotEmpty private String artifactStreamType;
  private String artifactStreamId;
  private int limit;

  // These fields are used only during artifact collection and cleanup.
  private boolean isCollection;
  // Unique key representing build numbers already present in the DB. It stores different things for different artifact
  // stream types like buildNo, revision or artifactPath.
  private Set<String> savedBuildDetailsKeys;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateCapabilities(settingValue, artifactStreamAttributes);
  }
}
