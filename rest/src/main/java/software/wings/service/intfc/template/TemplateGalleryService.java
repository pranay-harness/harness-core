package software.wings.service.intfc.template;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.template.TemplateGallery;
import software.wings.service.intfc.ownership.OwnedByAccount;

import javax.validation.Valid;

public interface TemplateGalleryService extends OwnedByAccount {
  /**
   * List template galleries, User can supply the filters
   * @param pageRequest
   * @return
   */
  PageResponse<TemplateGallery> list(PageRequest<TemplateGallery> pageRequest);

  TemplateGallery save(@Valid TemplateGallery templateGallery);

  TemplateGallery update(@Valid TemplateGallery templateGallery);

  void delete(String galleryUuid);

  /**
   * Gets gallery by Account Name  and Gallery Name
   * @param accountId
   * @param galleryName
   * @return
   */
  TemplateGallery get(@NotEmpty String accountId, @NotEmpty String galleryName);

  /**
   * Gets Template Gallery by uuid
   * @param uuid
   * @return
   */
  TemplateGallery get(@NotEmpty String uuid);

  /**
   * Get template galleries by accountId
   * @param accountId
   * @return
   */
  TemplateGallery getByAccount(@NotEmpty String accountId);

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
   * @return
   */
  TemplateGallery saveHarnessGallery();

  void copyHarnessTemplatesToAccount(@NotEmpty String accountId, @NotEmpty String accountName);

  void deleteAccountGalleryByName(String accountId, String galleryName);
}
