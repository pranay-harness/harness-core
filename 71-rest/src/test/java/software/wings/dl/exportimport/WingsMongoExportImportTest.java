package software.wings.dl.exportimport;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import io.harness.category.element.UnitTests;
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
    appId =
        wingsPersistence.save(Application.Builder.anApplication().withName(appName).withAccountId(accountId).build());
  }

  @After
  public void teardown() {
    // Cleanup
    wingsPersistence.delete(accountId, Application.class, appId);
  }

  @Test
  @Category(UnitTests.class)
  public void testCollectionExportImport() {
    String collectionName = Application.class.getAnnotation(Entity.class).value();

    List<String> records = mongoExportImport.exportRecords(new BasicDBObject("accountId", accountId), collectionName);
    assertNotNull(records);
    assertEquals(1, records.size());

    String appJson = records.get(0);
    logger.info("Application JSON: " + appJson);

    // Remove the inserted application to make space for re-importing.
    wingsPersistence.delete(accountId, Application.class, appId);

    mongoExportImport.importRecords(collectionName, records, ImportMode.UPSERT);

    Application application = wingsPersistence.get(Application.class, appId);
    assertNotNull(application);
    assertEquals(appName, application.getName());
  }
}
