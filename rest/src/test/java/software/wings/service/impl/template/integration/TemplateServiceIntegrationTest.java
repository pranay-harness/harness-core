package software.wings.service.impl.template.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.template.TemplateType.SSH;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.LATEST_TAG;
import static software.wings.common.TemplateConstants.TOMCAT_WAR_INSTALL_PATH;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_DEC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_NAME;

import com.google.inject.Inject;

import migrations.all.SystemTemplateGalleryMigration;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ExpressionEvaluator;
import software.wings.rules.Integration;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.sm.states.HttpState;

import java.util.List;
import java.util.regex.Matcher;

@Integration
@Ignore
public class TemplateServiceIntegrationTest extends WingsBaseTest {
  @Inject private TemplateFolderService templateFolderService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private SystemTemplateGalleryMigration migration;
  @Inject private TemplateService templateService;

  @Test
  public void shouldMigrateGallery() {
    migration.migrate();
  }

  @Test
  public void shouldLoadTemplateFolders() {
    wingsPersistence.delete(
        wingsPersistence.createQuery(TemplateFolder.class).filter(ACCOUNT_ID_KEY, GLOBAL_ACCOUNT_ID));
    templateFolderService.loadDefaultTemplateFolders();
  }

  @Test
  public void shouldGetTemplateTree() {
    TemplateFolder templateFolder = templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, null, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
  }

  @Test
  public void shouldGetTemplateTreeByKeyword() {
    TemplateFolder templateFolder = templateFolderService.getTemplateTree("kmpySmUISimoRrJL6NL73w", "Install", null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
  }

  @Test
  public void shouldCopyHarnessTemplateFolders() {
    List<Account> accounts = accountService.list(aPageRequest().addFilter(APP_ID_KEY, EQ, GLOBAL_APP_ID).build());
    accounts.forEach(account -> {
      wingsPersistence.delete(
          wingsPersistence.createQuery(TemplateFolder.class).filter("accountId", account.getUuid()));
      templateFolderService.copyHarnessTemplateFolders(account.getUuid());
    });
  }

  @Test
  public void shouldGetAccountTemplateTree() {
    List<Account> accounts = accountService.list(aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build());
    accounts.forEach(account -> {
      TemplateFolder templateFolder = templateFolderService.getTemplateTree(account.getUuid(), null, null);
      assertThat(templateFolder).isNotNull();
      assertThat(templateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
      assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
      assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
    });
  }

  @Test
  public void shouldLoadDefaultCommandTemplates() {
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID);
  }

  @Test
  public void shouldLoadDefaultCommandTemplatesForAccount() {
    List<Account> accounts = accountService.list(aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build());
    accounts.forEach(account -> {
      wingsPersistence.delete(wingsPersistence.createQuery(Template.class).filter("accountId", account.getUuid()));
      templateService.loadDefaultTemplates(SSH, account.getUuid());
    });
  }

  @Test
  public void shouldLoadTomcatStandardInstallCommand() {
    templateService.loadYaml(SSH, TOMCAT_WAR_INSTALL_PATH, GLOBAL_ACCOUNT_ID);
  }

  @Test
  public void shouldUpdateLinkedEntities() {
    Template template = templateService.get("EtnnNloDR5i3cZoQ1LUW0Q");
    templateService.update(template);
  }

  @Test
  public void shouldUpdateLinkedServiceCommandEntities() {
    Template template = templateService.get("EtnnNloDR5i3cZoQ1LUW0Q");
    templateService.update(template);
  }

  @Test
  public void shouldTestExpression() {
    String content = "${F5_URL}/abdcc/";
    Matcher matcher = ExpressionEvaluator.wingsVariablePattern.matcher(content);
    while (matcher.find()) {
      String name = matcher.group(0);
      assertThat(name).isEqualTo(content);
    }
  }

  @Test
  public void shouldCreateHttpTemplate() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    HttpTemplate httpTemplate = HttpTemplate.builder()
                                    .url("$workflow.variables.F5_URL}/mgmt/tm/${foo}/members")
                                    .assertion("200 ok")
                                    .body("{ \"kind\": ${LB_TYPE}}")
                                    .header("Authorization:${workflow.variables.F5_AUTH_BASE64}")
                                    .build();

    Template template = Template.builder()
                            .templateObject(httpTemplate)
                            .folderId(parentFolder.getUuid())
                            .appId(GLOBAL_APP_ID)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .name("Enable Instance4")
                            .build();
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getKeywords()).isNotEmpty();
    assertThat(savedTemplate.getKeywords()).contains(LATEST_TAG, template.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    assertThat(savedTemplate.getVariables()).isNotEmpty();
    assertThat(savedTemplate.getVariables())
        .extracting("name")
        .contains("${foo}", "${LB_TYPE}", "${workflow.variables.F5_AUTH_BASE64}");
    HttpState savedHttpTemplate = (HttpState) savedTemplate.getTemplateObject();
    assertThat(savedHttpTemplate).isNotNull();
    assertThat(savedHttpTemplate.getAssertion()).isNotEmpty();
  }
}
