package io.harness.migrations.all;

import static io.harness.rule.OwnerRule.AADITI;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.migrations.seedata.IISInstallCommandMigration;
import io.harness.rule.Owner;

import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateType;
import software.wings.service.impl.template.TemplateBaseTestHelper;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class IISInstallCommandMigrationTest extends TemplateBaseTestHelper {
  @InjectMocks @Inject private IISInstallCommandMigration iisInstallCommandMigration;

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testMigration() {
    iisInstallCommandMigration.migrate();
    TemplateFolder folder =
        templateService.getTemplateTree(GLOBAL_ACCOUNT_ID, "powershell", asList(TemplateType.SSH.name()));
    assertThat(folder).isNotNull();
  }
}
