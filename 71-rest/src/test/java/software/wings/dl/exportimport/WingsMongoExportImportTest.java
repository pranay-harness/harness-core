package software.wings.dl.exportimport;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.annotations.Entity;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;

import java.util.List;

/**
 * @author marklu on 10/24/18
 */
@Slf4j
public class WingsMongoExportImportTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WingsMongoExportImport mongoExportImport;

  private String accountId;
  private String appId;
  private String appName;

  @Before
  public void setup() {
    accountId = generateUuid();
    appName = generateUuid();
    appId = wingsPersistence.save(Application.Builder.anApplication().name(appName).accountId(accountId).build());
  }

  @After
  public void teardown() {
    // Cleanup
    wingsPersistence.delete(accountId, Application.class, appId);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testCollectionExportImport() {
    String collectionName = Application.class.getAnnotation(Entity.class).value();

    List<String> records = mongoExportImport.exportRecords(new BasicDBObject("accountId", accountId), collectionName);
    assertThat(records).isNotNull();
    assertThat(records).hasSize(1);

    String appJson = records.get(0);
    logger.info("Application JSON: " + appJson);

    // Remove the inserted application to make space for re-importing.
    wingsPersistence.delete(accountId, Application.class, appId);

    mongoExportImport.importRecords(collectionName, records, ImportMode.UPSERT);

    Application application = wingsPersistence.get(Application.class, appId);
    assertThat(application).isNotNull();
    assertThat(application.getName()).isEqualTo(appName);
  }
}
