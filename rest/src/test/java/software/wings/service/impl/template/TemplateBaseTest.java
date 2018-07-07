package software.wings.service.impl.template;

import static java.util.Arrays.asList;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.template.TemplateType.SSH;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_GALLERY_DESC;

import com.google.inject.Inject;

import org.junit.Before;
import software.wings.WingsBaseTest;
import software.wings.beans.template.TemplateGallery;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

public class TemplateBaseTest extends WingsBaseTest {
  @Inject protected TemplateFolderService templateFolderService;
  @Inject protected TemplateService templateService;
  @Inject protected TemplateGalleryService templateGalleryService;

  @Before
  public void setUp() {
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    if (templateGallery != null) {
      templateGalleryService.delete(templateGallery.getUuid());
    }
    saveTemplateGallery();
    templateFolderService.loadDefaultTemplateFolders();
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID);
  }

  public void saveTemplateGallery() {
    templateGalleryService.save(prepareTemplateGallery());
  }

  protected TemplateGallery prepareTemplateGallery() {
    return TemplateGallery.builder()
        .name(HARNESS_GALLERY)
        .accountId(GLOBAL_ACCOUNT_ID)
        .description(TEMPLATE_GALLERY_DESC)
        .accountId(GLOBAL_ACCOUNT_ID)
        .appId(GLOBAL_APP_ID)
        .keywords(asList("CD"))
        .build();
  }
}
