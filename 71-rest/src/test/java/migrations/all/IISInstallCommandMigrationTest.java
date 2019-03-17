package migrations.all;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import migrations.seedata.IISInstallCommandMigration;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateType;
import software.wings.service.impl.template.TemplateBaseTest;

public class IISInstallCommandMigrationTest extends TemplateBaseTest {
  @InjectMocks @Inject private IISInstallCommandMigration iisInstallCommandMigration;

  @Test
  @Category(UnitTests.class)
  public void testMigration() {
    iisInstallCommandMigration.migrate();
    TemplateFolder folder =
        templateService.getTemplateTree(GLOBAL_ACCOUNT_ID, "powershell", asList(TemplateType.SSH.name()));
    assertThat(folder).isNotNull();
  }
}
