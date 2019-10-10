package software.wings.search.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

import java.util.ArrayList;
import java.util.List;

public class SynchronousElasticsearchDaoTest extends WingsBaseTest {
  @Mock private ElasticsearchIndexManager elasticsearchIndexManager;
  @Mock private ElasticsearchDao elasticsearchDao;
  @Inject @InjectMocks private SynchronousElasticsearchDao synchronousElasticsearchDao;
  private static final String ENTITY_TYPE = "entity_type";
  private static final String ENTITY_ID = "entity_id";

  @Before
  public void setup() {
    when(elasticsearchIndexManager.getIndexName(ENTITY_TYPE)).thenReturn(ENTITY_TYPE);
  }

  @Test
  @Category(UnitTests.class)
  public void testInsertDocument() {
    String entityJson = "entity_json";
    when(elasticsearchDao.insertDocument(ENTITY_TYPE, ENTITY_ID, entityJson)).thenReturn(true);
    boolean isSuccessful = synchronousElasticsearchDao.insertDocument(ENTITY_TYPE, ENTITY_ID, entityJson);
    assertThat(isSuccessful).isEqualTo(true);
  }

  @Test
  @Category(UnitTests.class)
  public void testUpsertDocuemnt() {
    String entityJson = "entity_json";
    when(elasticsearchDao.upsertDocument(ENTITY_TYPE, ENTITY_ID, entityJson)).thenReturn(true);
    boolean isSuccessful = synchronousElasticsearchDao.upsertDocument(ENTITY_TYPE, ENTITY_ID, entityJson);
    assertThat(isSuccessful).isEqualTo(true);
  }

  @Test
  @Category(UnitTests.class)
  public void testNestedQuery() {
    String value = "value";
    List<String> arrayList = new ArrayList<>();
    arrayList.add(value);
    when(elasticsearchDao.nestedQuery(ENTITY_TYPE, ENTITY_ID, value)).thenReturn(arrayList);
    List<String> resultList = synchronousElasticsearchDao.nestedQuery(ENTITY_TYPE, ENTITY_ID, value);
    assertThat(resultList).isNotNull();
    assertThat(resultList.size()).isEqualTo(1);
    assertThat(resultList.get(0)).isEqualTo(value);

    String value1 = "value1";
    when(elasticsearchDao.nestedQuery(ENTITY_TYPE, ENTITY_ID, value1)).thenThrow(new RuntimeException());
    List<String> resultList1 = synchronousElasticsearchDao.nestedQuery(ENTITY_TYPE, ENTITY_ID, value1);
    assertThat(resultList1).isNotNull();
    assertThat(resultList1.size()).isEqualTo(0);
  }
}
