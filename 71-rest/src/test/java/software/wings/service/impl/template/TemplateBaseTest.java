package software.wings.service.impl.template;

import com.google.inject.Inject;

import org.junit.Before;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

public class TemplateBaseTest extends WingsBaseTest {
  @Inject protected TemplateFolderService templateFolderService;
  @Inject protected TemplateService templateService;
  @Inject protected TemplateGalleryService templateGalleryService;

  @Before
  public void setUp() {
    templateGalleryService.loadHarnessGallery();
  }
}
