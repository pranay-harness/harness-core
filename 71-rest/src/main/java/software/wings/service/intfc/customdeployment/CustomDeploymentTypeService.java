package software.wings.service.intfc.customdeployment;

import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.customdeployment.CustomDeploymentTypeDTO;
import software.wings.beans.template.deploymenttype.CustomDeploymentTypeTemplate;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CustomDeploymentTypeService {
  List<CustomDeploymentTypeDTO> list(@Nonnull String accountId, String appId, boolean withDetails);

  CustomDeploymentTypeDTO get(@Nonnull String accountId, @Nonnull String templateId, @Nullable String version);

  String fetchDeploymentTemplateUri(@NotBlank String templateUuid);

  String fetchDeploymentTemplateIdFromUri(@NotBlank String accountId, @NotBlank String templateUri);

  CustomDeploymentTypeTemplate fetchDeploymentTemplate(
      @NotBlank String accountId, @NotBlank String templateId, @Nullable String version);

  void putCustomDeploymentTypeNameIfApplicable(@Nonnull CustomDeploymentTypeAware entity);

  void putCustomDeploymentTypeNameIfApplicable(
      List<? extends CustomDeploymentTypeAware> entities, @NotBlank String accountId);
}
