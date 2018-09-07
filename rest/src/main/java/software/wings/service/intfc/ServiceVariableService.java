package software.wings.service.intfc;

import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByService;

import java.util.List;
import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
public interface ServiceVariableService extends OwnedByService {
  /**
   * List page response.
   *
   * @param request the request
   * @return the page response
   */
  PageResponse<ServiceVariable> list(PageRequest<ServiceVariable> request);

  /**
   * List page response.
   *
   * @param request the request
   * @param  maskEncryptedFields boolean
   * @return the page response
   */
  PageResponse<ServiceVariable> list(PageRequest<ServiceVariable> request, boolean maskEncryptedFields);

  /**
   * Save service variable.
   *
   * @param serviceVariable the service variable
   * @return the service variable
   */
  @ValidationGroups(Create.class) ServiceVariable save(@Valid ServiceVariable serviceVariable);

  /**
   * Get service variable.
   *
   * @param appId     the app id
   * @param settingId the setting id
   * @return the service variable
   */
  ServiceVariable get(@NotEmpty String appId, @NotEmpty String settingId);

  /**
   * Get service variable.
   *
   * @param appId     the app id
   * @param settingId the setting id
   * @param maskEncryptedFields boolean
   * @return the service variable
   */
  ServiceVariable get(@NotEmpty String appId, @NotEmpty String settingId, boolean maskEncryptedFields);

  /**
   * Update service variable.
   *
   * @param serviceVariable the service variable
   * @return the service variable
   */
  @ValidationGroups(Update.class) ServiceVariable update(@Valid ServiceVariable serviceVariable);

  /**
   * Delete.
   *
   * @param appId     the app id
   * @param settingId the setting id
   */
  void delete(@NotEmpty String appId, @NotEmpty String settingId);

  /**
   * Gets service variables for entity.
   *
   * @param appId      the app id
   * @param entityId   the entity id
   * @param maskEncryptedFields the boolean
   * @return the service variables for entity
   */
  List<ServiceVariable> getServiceVariablesForEntity(String appId, String entityId, boolean maskEncryptedFields);

  /**
   * Gets service variables by template.
   *
   * @param appId           the app id
   * @param envId           the env id
   * @param serviceTemplate the service template
   * @return the service variables by template
   */
  List<ServiceVariable> getServiceVariablesByTemplate(
      String appId, String envId, ServiceTemplate serviceTemplate, boolean maskEncryptedFields);

  /**
   * Delete by template id.
   *
   * @param appId             the app id
   * @param serviceTemplateId the service template id
   */
  void deleteByTemplateId(String appId, String serviceTemplateId);

  /**
   * Checks and updates the search tags for secrets.
   * @param accountId
   */
  int updateSearchTagsForSecrets(String accountId);

  @ValidationGroups(Update.class) ServiceVariable update(@Valid ServiceVariable serviceVariable, boolean syncFromGit);

  @ValidationGroups(Create.class) ServiceVariable save(@Valid ServiceVariable serviceVariable, boolean syncFromGit);

  void delete(@NotEmpty String appId, @NotEmpty String settingId, boolean syncFromGit);
}
