package software.wings.beans.template;

import com.google.inject.Inject;

import software.wings.beans.template.TemplateGallery.GalleryKey;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.utils.Utils;

public class TemplateGalleryHelper {
  @Inject TemplateGalleryService templateGalleryService;

  public TemplateGallery getGalleryByGalleryKey(String galleryKey, String accountId) {
    GalleryKey galleryKeyEnum = Utils.getEnumFromString(GalleryKey.class, galleryKey);
    return templateGalleryService.getByAccount(accountId, galleryKeyEnum);
  }

  public String getGalleryKeyNameByGalleryId(String galleryId) {
    TemplateGallery templateGallery = templateGalleryService.get(galleryId);
    return templateGallery.getGalleryKey();
  }
}
