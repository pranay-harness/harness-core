/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.template;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;

import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateType;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.List;
import javax.validation.Valid;
import org.hibernate.validator.constraints.NotEmpty;

public interface TemplateGalleryService extends OwnedByAccount {
  /**
   * List template galleries, User can supply the filters
   *
   * @param pageRequest
   * @return
   */
  PageResponse<TemplateGallery> list(PageRequest<TemplateGallery> pageRequest);

  TemplateGallery save(@Valid TemplateGallery templateGallery);

  TemplateGallery update(@Valid TemplateGallery templateGallery);

  void delete(String galleryUuid);

  /**
   * Gets gallery by Account Name  and Gallery Name
   *
   * @param accountId
   * @param galleryName
   * @return
   */
  //  @Deprecated should not be used in future code,
  TemplateGallery get(@NotEmpty String accountId, @NotEmpty String galleryName);

  /**
   * Gets Template Gallery by uuid
   *
   * @param uuid
   * @return
   */
  TemplateGallery get(@NotEmpty String uuid);

  TemplateGallery.GalleryKey getAccountGalleryKey();

  /**
   * Get template galleries by accountId
   *
   * @param accountId
   * @param galleryKey
   * @return
   */
  TemplateGallery getByAccount(@NotEmpty String accountId, TemplateGallery.GalleryKey galleryKey);

  TemplateGallery getByAccount(@NotEmpty String accountId, String galleryId);

  /**
   * Loads default harness gallery with the
   */
  void loadHarnessGallery();

  /**
   * Copies Harness template
   */
  void copyHarnessTemplates();

  /**
   * Saves Harness gallery
   *
   * @return
   */
  TemplateGallery saveHarnessGallery();

  void saveHarnessCommandLibraryGalleryToAccount(String accountId, String accountName);

  void copyHarnessTemplatesToAccount(@NotEmpty String accountId, @NotEmpty String accountName);

  void copyHarnessTemplatesToAccountV2(@NotEmpty String accountId, @NotEmpty String accountName);

  void createCommandLibraryGallery();

  //  @Deprecated should not be used in future code,
  void deleteAccountGalleryByName(String accountId, String galleryName);

  void copyHarnessTemplateFromGalleryToAccounts(
      String sourceFolderPath, TemplateType templateType, String templateName, String yamlFilePath);

  void copyHarnessTemplateFromGalleryToAccount(String sourceFolderPath, TemplateType templateType, String templateName,
      String yamlFilePath, String accountId, String accountName, String galleryId);

  void copyNewVersionFromGlobalToAllAccounts(Template template, String keyword);

  void copyNewFolderAndTemplatesFromGlobalToAccounts(
      String sourceFolderPath, TemplateType templateType, List<String> yamlFilePaths);
}
