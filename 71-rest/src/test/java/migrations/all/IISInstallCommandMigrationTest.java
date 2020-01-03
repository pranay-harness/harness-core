package migrations.all;

import static io.harness.rule.OwnerRule.AADITI;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import migrations.seedata.IISInstallCommandMigration;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateType;
import software.wings.service.impl.template.TemplateBaseTestHelper;

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
