package software.wings.search.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.search.entities.application.ApplicationSearchEntity;

import java.io.IOException;

@Slf4j
public class ElasticsearchEntityBulkMigratorTest extends WingsBaseTest {
  @Mock private ElasticsearchIndexManager elasticsearchIndexManager;
  @Mock private SearchDao searchDao;
  @Inject @InjectMocks private ApplicationSearchEntity aSearchEntity;
  @Inject @InjectMocks private ElasticsearchEntityBulkMigrator elasticsearchEntityBulkMigrator;

  @Before
  public void setup() {
    Application application = new Application();
    application.setName("first application");
    application.setDescription("Application to test bulk sync");
    wingsPersistence.save(application);
  }

  @Test
  @Category(UnitTests.class)
  public void testSearchEntityBulkMigration() throws IOException {
    when(elasticsearchIndexManager.getIndexName(aSearchEntity.getType()))
        .thenReturn(aSearchEntity.getType().concat("_default"));

    when(elasticsearchIndexManager.isIndexPresent(aSearchEntity.getType())).thenReturn(true);
    AcknowledgedResponse acknowledgedResponse = new AcknowledgedResponse(true);
    when(elasticsearchIndexManager.deleteIndex(aSearchEntity.getType())).thenReturn(acknowledgedResponse);
    CreateIndexResponse createIndexResponse =
        new CreateIndexResponse(true, true, aSearchEntity.getType().concat("_default"));
    when(elasticsearchIndexManager.createIndex(eq(aSearchEntity.getType()), (String) notNull()))
        .thenReturn(createIndexResponse);
    when(searchDao.insertDocument(eq(aSearchEntity.getType()), (String) notNull(), (String) notNull()))
        .thenReturn(true);

    boolean isMigrated = elasticsearchEntityBulkMigrator.runBulkMigration(aSearchEntity);
    assertThat(isMigrated).isEqualTo(true);
  }
}
