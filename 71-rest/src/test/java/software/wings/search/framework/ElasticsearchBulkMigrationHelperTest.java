package software.wings.search.framework;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.rest.RestStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.search.entities.application.ApplicationSearchEntity;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class ElasticsearchBulkMigrationHelperTest extends WingsBaseTest {
  @Mock private ElasticsearchIndexManager elasticsearchIndexManager;
  @Mock private ElasticsearchClient elasticsearchClient;
  @Inject @InjectMocks private ApplicationSearchEntity aSearchEntity;
  @Inject @InjectMocks private ElasticsearchBulkMigrationHelper elasticsearchBulkMigrationHelper;

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testSearchEntityBulkMigration() throws IOException {
    Account account = new Account();
    String accountId = wingsPersistence.save(account);
    account.setUuid(accountId);

    Application application = new Application();
    application.setName("first application");
    application.setDescription("Application to test bulk sync");
    application.setAccountId(accountId);
    String applicationUuid = wingsPersistence.save(application);
    application.setUuid(applicationUuid);

    Set<SearchEntity<?>> searchEntities = new HashSet<>();
    searchEntities.add(aSearchEntity);

    String newIndexName = "newIndexName";
    String oldIndexName = "oldIndexName";
    String oldVersion = "0.1";
    String newVersion = "0.2";

    ElasticsearchBulkMigrationJob elasticsearchBulkMigrationJob =
        ElasticsearchBulkMigrationJob.builder()
            .entityClass(aSearchEntity.getClass().getCanonicalName())
            .newIndexName(newIndexName)
            .oldIndexName(oldIndexName)
            .fromVersion(oldVersion)
            .toVersion(newVersion)
            .build();

    wingsPersistence.save(elasticsearchBulkMigrationJob);

    IndexResponse indexResponse = mock(IndexResponse.class);
    when(indexResponse.status()).thenReturn(RestStatus.OK);
    when(elasticsearchClient.index(any())).thenReturn(indexResponse);
    when(elasticsearchIndexManager.createIndex(eq(newIndexName), (String) notNull())).thenReturn(true);
    when(elasticsearchIndexManager.getAliasName(aSearchEntity.getType())).thenReturn(aSearchEntity.getType());
    when(elasticsearchIndexManager.attachIndexToAlias(aSearchEntity.getType(), newIndexName)).thenReturn(true);
    when(elasticsearchIndexManager.removeIndexFromAlias(oldIndexName)).thenReturn(true);

    boolean isMigrated = elasticsearchBulkMigrationHelper.doBulkSync(searchEntities);
    assertThat(isMigrated).isEqualTo(true);

    verify(elasticsearchClient, times(1)).index(any());
    verify(elasticsearchIndexManager, times(1)).createIndex(eq(newIndexName), any());
    verify(elasticsearchIndexManager, times(1)).getAliasName(aSearchEntity.getType());
    verify(elasticsearchIndexManager, times(1)).attachIndexToAlias(aSearchEntity.getType(), newIndexName);
    verify(elasticsearchIndexManager, times(1)).removeIndexFromAlias(oldIndexName);
  }
}
