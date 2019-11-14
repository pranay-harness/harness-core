package software.wings.search.framework;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.rest.RestStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

import java.io.IOException;

public class ElasticsearchDaoTest extends WingsBaseTest {
  @Mock ElasticsearchClient elasticsearchClient;
  @Mock ElasticsearchIndexManager elasticsearchIndexManager;
  @Inject @InjectMocks ElasticsearchDao elasticsearchDao;

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void upsertDocumentTest() throws IOException {
    String entityType = "entityType";
    String entityId = "entityId";
    String entityJson = "entityJson";
    UpdateResponse updateResponse = mock(UpdateResponse.class);

    when(elasticsearchIndexManager.getIndexName(entityType)).thenReturn(entityType);
    when(elasticsearchClient.update(any())).thenReturn(updateResponse);
    when(updateResponse.status()).thenReturn(RestStatus.OK);

    boolean isSuccessful = elasticsearchDao.upsertDocument(entityType, entityId, entityJson);
    assertThat(isSuccessful).isEqualTo(true);

    verify(elasticsearchIndexManager, times(1)).getIndexName(entityType);

    ArgumentCaptor<UpdateRequest> captor = ArgumentCaptor.forClass(UpdateRequest.class);
    verify(elasticsearchClient, times(1)).update(captor.capture());
    UpdateRequest updateRequest = captor.getValue();
    assertThat(updateRequest.id()).isEqualTo(entityId);
    assertThat(updateRequest.docAsUpsert()).isEqualTo(true);
    assertThat(updateRequest.retryOnConflict()).isEqualTo(3);
    assertThat(updateRequest.getRefreshPolicy()).isEqualTo(RefreshPolicy.WAIT_UNTIL);

    when(elasticsearchClient.update(any())).thenThrow(new IOException());
    isSuccessful = elasticsearchDao.upsertDocument(entityType, entityId, entityJson);
    assertThat(isSuccessful).isEqualTo(false);
    verify(elasticsearchClient, times(2)).update(any());
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void deleteDocumentTest() throws IOException {
    String entityType = "entityType";
    String entityId = "entityId";
    DeleteResponse deleteResponse = mock(DeleteResponse.class);

    when(elasticsearchIndexManager.getIndexName(entityType)).thenReturn(entityType);
    when(elasticsearchClient.delete(any())).thenReturn(deleteResponse);
    when(deleteResponse.status()).thenReturn(RestStatus.OK);

    boolean isSuccessful = elasticsearchDao.deleteDocument(entityType, entityId);
    assertThat(isSuccessful).isEqualTo(true);

    verify(elasticsearchIndexManager, times(1)).getIndexName(entityType);

    ArgumentCaptor<DeleteRequest> captor = ArgumentCaptor.forClass(DeleteRequest.class);
    verify(elasticsearchClient, times(1)).delete(captor.capture());
    DeleteRequest deleteRequest = captor.getValue();
    assertThat(deleteRequest.index()).isEqualTo(entityType);
    assertThat(deleteRequest.id()).isEqualTo(entityId);
    assertThat(deleteRequest.getRefreshPolicy()).isEqualTo(RefreshPolicy.WAIT_UNTIL);

    when(elasticsearchClient.delete(any())).thenThrow(new IOException());
    isSuccessful = elasticsearchDao.deleteDocument(entityType, entityId);
    assertThat(isSuccessful).isEqualTo(false);
    verify(elasticsearchClient, times(2)).delete(any());
  }
}
