package software.wings.service.impl.yaml.service;

import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.appmanifest.AppManifestKind.PCF_OVERRIDE;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.GeneralException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.WingsBaseTest;
import software.wings.beans.template.TemplateFolder;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateService;

import java.util.Arrays;

public class YamlHelperTest extends WingsBaseTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock TemplateService templateService;
  @Mock TemplateFolderService templateFolderService;
  @InjectMocks @Inject private YamlHelper yamlHelper;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetAppManifestKindFromPath() {
    assertThat(
        yamlHelper.getAppManifestKindFromPath("Setup/Applications/App1/Environments/env1/PCF Overrides/Index.yaml"))
        .isEqualTo(PCF_OVERRIDE);

    assertThat(
        yamlHelper.getAppManifestKindFromPath("Setup/Applications/App1/Environments/env1/PCF Overrides/Services"))
        .isEqualTo(PCF_OVERRIDE);
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetTemplateFolderForYamlFilePath() {
    TemplateFolder templateFolder = TemplateFolder.builder()
                                        .name("Harness")
                                        .children(Arrays.asList(TemplateFolder.builder().name("Children").build()))
                                        .build();
    when(templateService.getTemplateTree(any(), any(), any(), any())).thenReturn(templateFolder);

    TemplateFolder returnTemplateFolder = yamlHelper.getTemplateFolderForYamlFilePath(GLOBAL_ACCOUNT_ID,
        "Setup/Template Library/" + templateFolder.getName() + "/" + templateFolder.getChildren().get(0).getName()
            + "/test.yaml",
        GLOBAL_APP_ID);
    assertThat(returnTemplateFolder.getName()).isEqualTo(templateFolder.getChildren().get(0).getName());
    assertThatThrownBy(() -> yamlHelper.getTemplateFolderForYamlFilePath(GLOBAL_ACCOUNT_ID, "random", GLOBAL_APP_ID))
        .isInstanceOf(GeneralException.class);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testEnsureTemplateFolder() {
    // Case 1: When folder already exists.
    TemplateFolder templateFolder = TemplateFolder.builder()
                                        .name("Harness")
                                        .children(Arrays.asList(TemplateFolder.builder().name("Children").build()))
                                        .build();
    doReturn(templateFolder).when(templateService).getTemplateTree(anyString(), anyString(), anyString(), anyList());
    TemplateFolder returnTemplateFolder = yamlHelper.ensureTemplateFolder(GLOBAL_ACCOUNT_ID,
        "Setup/Template Library/" + templateFolder.getName() + "/" + templateFolder.getChildren().get(0).getName()
            + "/test.yaml",
        GLOBAL_APP_ID, "random");
    assertThat(returnTemplateFolder.getName()).isEqualTo(templateFolder.getChildren().get(0).getName());

    // Case 2: When folder is to be created.
    String newFolderName = "newFolder";
    TemplateFolder expectedTemplateFolder = TemplateFolder.builder().name(newFolderName).appId(GLOBAL_APP_ID).build();
    when(templateFolderService.saveSafelyAndGet(anyObject(), anyString())).thenReturn(expectedTemplateFolder);
    TemplateFolder returnTemplateFolderCase2 = yamlHelper.ensureTemplateFolder(GLOBAL_ACCOUNT_ID,
        "Setup/Template Library/" + templateFolder.getName() + "/" + templateFolder.getChildren().get(0).getName() + "/"
            + newFolderName + "/test.yaml",
        GLOBAL_APP_ID, "random");
    assertThat(returnTemplateFolderCase2.getName()).isEqualTo(newFolderName);
  }
}
