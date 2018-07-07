package software.wings.service.intfc.template;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.template.TemplateGallery;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import javax.validation.Valid;

public interface TemplateGalleryService {
  /**
   *
   * @param pageRequest
   * @return
   */
  PageResponse<TemplateGallery> list(PageRequest<TemplateGallery> pageRequest);

  /**
   *
   * @param templateGallery
   * @return
   */
  @ValidationGroups(Create.class) TemplateGallery save(@Valid TemplateGallery templateGallery);

  /**
   *
   * @param templateGallery
   * @return
   */
  @ValidationGroups(Update.class) TemplateGallery update(@Valid TemplateGallery templateGallery);

  /**
   *
   * @param galleryUuid
   */
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
}
