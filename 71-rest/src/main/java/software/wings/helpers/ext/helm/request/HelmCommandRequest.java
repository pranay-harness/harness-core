package software.wings.helpers.ext.helm.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.GitConfig;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;

/**
 * Created by anubhaw on 3/22/18.
 */
@Data
@AllArgsConstructor
public class HelmCommandRequest {
  @NotEmpty private HelmCommandType helmCommandType;
  private String accountId;
  private String appId;
  private String kubeConfigLocation;
  private String commandName;
  private String activityId;
  private ContainerServiceParams containerServiceParams;
  private String releaseName;
  private HelmChartSpecification chartSpecification;
  private String repoName;
  private GitConfig gitConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;

  public HelmCommandRequest(HelmCommandType helmCommandType) {
    this.helmCommandType = helmCommandType;
  }

  public enum HelmCommandType { INSTALL, ROLLBACK, LIST_RELEASE, RELEASE_HISTORY }
}
