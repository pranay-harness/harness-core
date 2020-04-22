package software.wings.service.impl.template;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.api.commandlibrary.EnrichedCommandVersionDTO;
import software.wings.beans.template.ImportedTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateGallery.GalleryKey;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.template.dto.HarnessImportedTemplateDetails;
import software.wings.service.intfc.template.TemplateService;

public class ImportedTemplateServiceImplTest extends TemplateBaseTestHelper {
  @Inject TemplateService templateService;
  @Spy @Inject private ImportedTemplateServiceImpl importedTemplateService;

  private String COMMAND_NAME = "commandName";
  private String COMMAND_STORE_NAME = "commandStoreName";

  public void mockItems() {
    MockitoAnnotations.initMocks(this);
    doReturn(CommandDTO.builder().commandStoreName(COMMAND_STORE_NAME).name(COMMAND_NAME).build())
        .when(importedTemplateService)
        .downloadAndGetCommandDTO(anyString(), anyString());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSaveWhenCommandVersionAlreadyExist() {
    mockItems();
    String version = "1.2";
    doReturn(makeCommandVersionDTO(version))
        .when(importedTemplateService)
        .downloadAndGetCommandVersionDTO(anyString(), anyString(), anyString());

    saveImportedTemplate(COMMAND_NAME, "1.2");

    assertThatThrownBy(()
                           -> importedTemplateService.getAndSaveImportedTemplate(
                               version, COMMAND_NAME, COMMAND_STORE_NAME, GLOBAL_ACCOUNT_ID));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSaveWhenDifferentVersionOfCommandExists() {
    mockItems();
    // Different version saved.
    Template savedTemplate = saveImportedTemplate(COMMAND_NAME, "1.1");
    String version = "2.2";
    doReturn(makeCommandVersionDTO(version))
        .when(importedTemplateService)
        .downloadAndGetCommandVersionDTO(anyString(), anyString(), anyString());

    Template updatedTemplate = importedTemplateService.getAndSaveImportedTemplate(
        version, COMMAND_NAME, COMMAND_STORE_NAME, GLOBAL_ACCOUNT_ID);

    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getUuid()).isEqualTo(savedTemplate.getUuid());
    assertThat(updatedTemplate.getImportedTemplateDetails()).isInstanceOf(HarnessImportedTemplateDetails.class);
    assertThat(((HarnessImportedTemplateDetails) updatedTemplate.getImportedTemplateDetails()).getCommandName())
        .isEqualTo(COMMAND_NAME);
    assertThat(((HarnessImportedTemplateDetails) updatedTemplate.getImportedTemplateDetails()).getCommandVersion())
        .isEqualTo("2.2");
    assertThat(((HarnessImportedTemplateDetails) updatedTemplate.getImportedTemplateDetails()).getCommandStoreName())
        .isEqualTo(COMMAND_STORE_NAME);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSaveWhenFirstVersionOfCommandIsDownloaded() {
    mockItems();
    String version = "1.2";
    doReturn(makeCommandVersionDTO(version))
        .when(importedTemplateService)
        .downloadAndGetCommandVersionDTO(anyString(), anyString(), anyString());

    assertThat(importedTemplateService.getAndSaveImportedTemplate(
                   version, COMMAND_NAME, COMMAND_STORE_NAME, GLOBAL_ACCOUNT_ID))
        .isNotNull();
  }

  private Template saveImportedTemplate(String templateName, String version) {
    HarnessImportedTemplateDetails harnessImportedTemplateDetails = HarnessImportedTemplateDetails.builder()
                                                                        .commandName(COMMAND_NAME)
                                                                        .commandStoreName(COMMAND_STORE_NAME)
                                                                        .commandVersion(version)
                                                                        .build();
    TemplateGallery templateGallery =
        templateGalleryService.getByAccount(GLOBAL_ACCOUNT_ID, GalleryKey.HARNESS_COMMAND_LIBRARY_GALLERY);

    Template template = Template.builder()
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .appId(GLOBAL_APP_ID)
                            .name(templateName)
                            .templateObject(HttpTemplate.builder().build())
                            .galleryId(templateGallery.getUuid())
                            .importedTemplateDetails(harnessImportedTemplateDetails)
                            .build();
    String templateId = templateService.saveReferenceTemplate(template).getUuid();
    ImportedTemplate importedTemplate = ImportedTemplate.builder()
                                            .templateId(templateId)
                                            .name(COMMAND_NAME)
                                            .appId(GLOBAL_APP_ID)
                                            .accountId(GLOBAL_ACCOUNT_ID)
                                            .commandName(COMMAND_NAME)
                                            .commandStoreName(COMMAND_STORE_NAME)
                                            .build();
    wingsPersistence.save(importedTemplate);
    return template;
  }

  private EnrichedCommandVersionDTO makeCommandVersionDTO(String version) {
    return EnrichedCommandVersionDTO.builder()
        .commandName(COMMAND_NAME)
        .commandStoreName(COMMAND_STORE_NAME)
        .description("desc")
        .templateObject(HttpTemplate.builder().build())
        .version(version)
        .build();
  }
}
