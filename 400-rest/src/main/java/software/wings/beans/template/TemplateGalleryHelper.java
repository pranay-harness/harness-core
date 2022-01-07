/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.template;

import software.wings.beans.template.TemplateGallery.GalleryKey;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.utils.Utils;

import com.google.inject.Inject;

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
