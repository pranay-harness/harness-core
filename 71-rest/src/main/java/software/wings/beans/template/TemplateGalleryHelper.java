package software.wings.beans.template;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import software.wings.beans.template.TemplateGallery.GalleryKey;
import software.wings.service.intfc.template.TemplateGalleryService;

public class TemplateGalleryHelper {
  @Inject TemplateGalleryService templateGalleryService;

  public TemplateGallery getGalleryIdByGalleryKey(String galleryKey, String accountId) {
    GalleryKey galleryKeyEnum = getGalleryEnumFromString(galleryKey);
    return templateGalleryService.getByAccount(accountId, galleryKeyEnum);
  }

  public String getGalleryKeyNameByGalleryId(String galleryId) {
    TemplateGallery templateGallery = templateGalleryService.get(galleryId);
    return templateGallery.getGalleryKey();
  }

  private GalleryKey getGalleryEnumFromString(String galleryKey) {
    if (GalleryKey.ACCOUNT_TEMPLATE_GALLERY.name().equals(galleryKey)) {
      return GalleryKey.ACCOUNT_TEMPLATE_GALLERY;
    } else if (GalleryKey.HARNESS_COMMAND_LIBRARY_GALLERY.name().equals(galleryKey)) {
      return GalleryKey.HARNESS_COMMAND_LIBRARY_GALLERY;
    }
    throw new InvalidRequestException("Given gallery Key doesn't exist.");
  }
}
