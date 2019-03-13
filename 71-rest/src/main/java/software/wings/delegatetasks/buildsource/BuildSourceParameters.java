package software.wings.delegatetasks.buildsource;

import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.settings.SettingValue;

import java.util.List;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class BuildSourceParameters implements TaskParameters {
  public enum BuildSourceRequestType { GET_BUILDS, GET_LAST_SUCCESSFUL_BUILD }

  @NotNull private BuildSourceRequestType buildSourceRequestType;
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @NotNull private SettingValue settingValue;
  @NotNull private ArtifactStreamAttributes artifactStreamAttributes;
  @NotNull private List<EncryptedDataDetail> encryptedDataDetails;
  @NotEmpty private String artifactStreamType;
  private int limit;
}
