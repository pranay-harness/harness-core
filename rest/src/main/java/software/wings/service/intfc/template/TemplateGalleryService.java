package software.wings.service.intfc.template;

import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.template.TemplateGallery;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByAccount;

import javax.validation.Valid;

public interface TemplateGalleryService extends OwnedByAccount {
  /**
   * List template galleries, User can supply the filters
   * @param pageRequest
   * @return
   */
  PageResponse<TemplateGallery> list(PageRequest<TemplateGallery> pageRequest);

  @ValidationGroups(Create.class) TemplateGallery save(@Valid TemplateGallery templateGallery);

  @ValidationGroups(Update.class) TemplateGallery update(@Valid TemplateGallery templateGallery);

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
