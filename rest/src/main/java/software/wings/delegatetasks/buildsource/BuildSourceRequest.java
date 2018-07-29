package software.wings.delegatetasks.buildsource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.settings.SettingValue;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 7/20/18.
 */
@Data
@AllArgsConstructor
@Builder
public class BuildSourceRequest {
  @NotNull private BuildSourceRequestType buildSourceRequestType;
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @NotNull private SettingValue settingValue;
  @NotNull private ArtifactStreamAttributes artifactStreamAttributes;
  @NotNull private List<EncryptedDataDetail> encryptedDataDetails;
  @NotEmpty private String artifactStreamType;
  private int limit;

  public enum BuildSourceRequestType { GET_BUILDS, GET_LAST_SUCCESSFUL_BUILD }

  public BuildSourceRequest(BuildSourceRequestType buildSourceRequestType) {
    this.buildSourceRequestType = buildSourceRequestType;
  }
}
