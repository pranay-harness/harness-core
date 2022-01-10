/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import io.harness.validation.Update;

import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.service.intfc.ownership.OwnedByEnvironment;
import software.wings.service.intfc.ownership.OwnedByService;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface ServiceVariableService extends OwnedByService, OwnedByEnvironment {
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
   * @param  encryptedFieldMode EncryptedFieldMode
   * @return the page response
   */
  PageResponse<ServiceVariable> list(PageRequest<ServiceVariable> request, EncryptedFieldMode encryptedFieldMode);

  /**
   * Save service variable.
   *
   * @param serviceVariable the service variable
   * @return the service variable
   */
  @ValidationGroups(Create.class) ServiceVariable save(@Valid ServiceVariable serviceVariable);

  ServiceVariable saveWithChecks(@NotEmpty String appId, ServiceVariable serviceVariable);

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
  ServiceVariable get(@NotEmpty String appId, @NotEmpty String settingId, EncryptedFieldMode encryptedFieldMode);

  /**
   * Update service variable.
   *
   * @param serviceVariable the service variable
   * @return the service variable
   */
  @ValidationGroups(Update.class) ServiceVariable update(@Valid ServiceVariable serviceVariable);

  ServiceVariable updateWithChecks(
      @NotEmpty String appId, @NotEmpty String serviceVariableId, ServiceVariable serviceVariable);

  /**
   * Delete.
   *
   * @param appId     the app id
   * @param settingId the setting id
   */
  void delete(@NotEmpty String appId, @NotEmpty String settingId);

  void deleteWithChecks(@NotEmpty String appId, @NotEmpty String settingId);

  enum EncryptedFieldMode {
    OBTAIN_VALUE,
    MASKED,
  }

  /**
   * Gets service variables for entity.
   *
   * @param appId      the app id
   * @param entityId   the entity id
   * @param maskEncryptedFields the boolean
   * @return the service variables for entity
   */
  List<ServiceVariable> getServiceVariablesForEntity(
      String appId, String entityId, EncryptedFieldMode encryptedFieldMode);

  /**
   * Gets service variables by template.
   *
   * @param appId           the app id
   * @param envId           the env id
   * @param serviceTemplate the service template
   * @return the service variables by template
   */
  List<ServiceVariable> getServiceVariablesByTemplate(
      String appId, String envId, ServiceTemplate serviceTemplate, EncryptedFieldMode encryptedFieldMode);

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

  void pushServiceVariablesToGit(@NotNull ServiceVariable serviceVariable);
}
