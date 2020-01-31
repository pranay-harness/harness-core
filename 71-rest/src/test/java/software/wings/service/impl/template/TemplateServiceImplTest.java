package software.wings.service.impl.template;

import static io.harness.rule.OwnerRule.ABHINAV;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.command.CommandType.START;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_CUSTOM_KEYWORD;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC_CHANGED;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.Event;
import software.wings.beans.FeatureName;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

public class TemplateServiceImplTest extends WingsBaseTest {
  private static final String MY_START_COMMAND = "My Start Command";

  @Mock public AuditServiceHelper auditServiceHelper;
  @Mock FeatureFlagService featureFlagService;
  @InjectMocks @Inject TemplateService templateService;
  @Inject protected TemplateGalleryService templateGalleryService;

  @Before
  public void setUp() {
    templateGalleryService.loadHarnessGallery();
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testAuditReporting() {
    // This test can be ignored once TEMPLATE_YAML_SUPPORT feature flag is enabled for all.
    checkAuditForUpdate();
    checkAuditForCreate();
    checkAuditForDelete();
  }

  private void checkAuditForCreate() {
    when(featureFlagService.isEnabled(FeatureName.TEMPLATE_YAML_SUPPORT, GLOBAL_ACCOUNT_ID)).thenReturn(false);

    Template savedTemplate = saveTemplate();

    verify(auditServiceHelper, atLeastOnce())
        .reportForAuditingUsingAccountId(
            eq(savedTemplate.getAccountId()), eq(null), any(Template.class), eq(Event.Type.CREATE));
  }

  private void checkAuditForDelete() {
    when(featureFlagService.isEnabled(FeatureName.TEMPLATE_YAML_SUPPORT, GLOBAL_ACCOUNT_ID)).thenReturn(false);
    Template savedTemplate = saveTemplate();

    templateService.delete(GLOBAL_ACCOUNT_ID, savedTemplate.getUuid());

    verify(auditServiceHelper, times(1))
        .reportDeleteForAuditingUsingAccountId(eq(savedTemplate.getAccountId()), any(Template.class));
  }

  private void checkAuditForUpdate() {
    when(featureFlagService.isEnabled(FeatureName.TEMPLATE_YAML_SUPPORT, GLOBAL_ACCOUNT_ID)).thenReturn(false);
    Template template = getSshCommandTemplate();
    Template savedTemplate = templateService.save(template);

    savedTemplate.setDescription(TEMPLATE_DESC_CHANGED);
    Template updatedTemplate = templateService.update(savedTemplate);

    verify(auditServiceHelper, atLeastOnce())
        .reportForAuditingUsingAccountId(
            eq(updatedTemplate.getAccountId()), eq(null), any(Template.class), any(Event.Type.class));
  }

  private Template saveTemplate() {
    return saveTemplate(MY_START_COMMAND, GLOBAL_APP_ID);
  }

  private Template saveTemplate(String name, String appId) {
    Template template = getSshCommandTemplate(name, appId);
    Template savedTemplate = templateService.save(template);

    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(appId);
    assertThat(savedTemplate.getKeywords()).isNotEmpty();
    assertThat(savedTemplate.getKeywords()).contains(template.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    SshCommandTemplate savedSshCommandTemplate = (SshCommandTemplate) savedTemplate.getTemplateObject();
    assertThat(savedSshCommandTemplate).isNotNull();
    assertThat(savedSshCommandTemplate.getCommandType()).isEqualTo(START);
    assertThat(savedSshCommandTemplate.getCommandUnits()).isNotEmpty();
    assertThat(savedSshCommandTemplate.getCommandUnits()).extracting(CommandUnit::getName).contains("Start");
    return savedTemplate;
  }

  private Template getSshCommandTemplate() {
    return getSshCommandTemplate(MY_START_COMMAND, GLOBAL_APP_ID);
  }

  private Template getSshCommandTemplate(String name, String appId) {
    SshCommandTemplate sshCommandTemplate = SshCommandTemplate.builder()
                                                .commandType(START)
                                                .commandUnits(asList(anExecCommandUnit()
                                                                         .withName("Start")
                                                                         .withCommandPath("/home/xxx/tomcat")
                                                                         .withCommandString("bin/startup.sh")
                                                                         .build()))
                                                .build();

    return Template.builder()
        .templateObject(sshCommandTemplate)
        .name(name)
        .description(TEMPLATE_DESC)
        .folderPath("Harness/Tomcat Commands")
        .keywords(ImmutableSet.of(TEMPLATE_CUSTOM_KEYWORD))
        .gallery(HARNESS_GALLERY)
        .appId(appId)
        .accountId(GLOBAL_ACCOUNT_ID)
        .build();
  }
}