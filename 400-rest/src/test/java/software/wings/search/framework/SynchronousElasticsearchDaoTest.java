/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.search.framework;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class SynchronousElasticsearchDaoTest extends WingsBaseTest {
  @Mock private ElasticsearchIndexManager elasticsearchIndexManager;
  @Mock private ElasticsearchDao elasticsearchDao;
  @Inject @InjectMocks private SynchronousElasticsearchDao synchronousElasticsearchDao;
  private static final String entityType = "entity_type";
  private static final String entityId = "entity_id";
  private static final String listToUpdate = "list_to_update";
  private static final String keyToUpdate = "key_to_update";
  private static final String documentId = generateUuid();
  private static final List<String> documentIds = Arrays.asList(generateUuid());
  private static final Map<String, Object> newElement = new HashMap<>();
  private static final long timestamp = System.currentTimeMillis();
  private static final int daysToRetain = 7;
  private static final int limit = 3;

  @Before
  public void setup() {
    when(elasticsearchIndexManager.getIndexName(entityType)).thenReturn(entityType);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testUpsertDocument() {
    String entityJson = "entity_json";
    when(elasticsearchDao.upsertDocument(entityType, entityId, entityJson)).thenReturn(true);
    boolean isSuccessful = synchronousElasticsearchDao.upsertDocument(entityType, entityId, entityJson);
    assertThat(isSuccessful).isEqualTo(true);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testNestedQuery() {
    String value = "value";
    List<String> arrayList = new ArrayList<>();
    arrayList.add(value);
    when(elasticsearchDao.nestedQuery(entityType, entityId, value)).thenReturn(arrayList);
    List<String> resultList = synchronousElasticsearchDao.nestedQuery(entityType, entityId, value);
    assertThat(resultList).isNotNull();
    assertThat(resultList.size()).isEqualTo(1);
    assertThat(resultList.get(0)).isEqualTo(value);

    String value1 = "value1";
    when(elasticsearchDao.nestedQuery(entityType, entityId, value1)).thenThrow(new RuntimeException());
    List<String> resultList1 = synchronousElasticsearchDao.nestedQuery(entityType, entityId, value1);
    assertThat(resultList1).isNotNull();
    assertThat(resultList1.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testAddTimestamp() {
    when(elasticsearchDao.addTimestamp(entityType, listToUpdate, documentId, timestamp, daysToRetain)).thenReturn(true);
    boolean isSuccessful =
        synchronousElasticsearchDao.addTimestamp(entityType, listToUpdate, documentId, timestamp, daysToRetain);
    assertThat(isSuccessful).isTrue();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testAddTimestamps() {
    when(elasticsearchDao.addTimestamp(entityType, listToUpdate, documentIds, timestamp, daysToRetain))
        .thenReturn(true);
    boolean isSuccessful =
        synchronousElasticsearchDao.addTimestamp(entityType, listToUpdate, documentIds, timestamp, daysToRetain);
    assertThat(isSuccessful).isTrue();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testDeleteDocument() {
    String entityJson = "entity_json";
    when(elasticsearchDao.deleteDocument(entityType, entityJson)).thenReturn(true);
    boolean isSuccessful = synchronousElasticsearchDao.deleteDocument(entityType, entityJson);
    assertThat(isSuccessful).isEqualTo(true);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testUpdateKeyInMultipleDocuments() {
    String filterKey = "filter_key";
    String newValue = "new_value";
    String filterValue = "filter_value";
    when(elasticsearchDao.updateKeyInMultipleDocuments(entityType, keyToUpdate, newValue, filterKey, filterValue))
        .thenReturn(true);
    boolean isSuccessful = synchronousElasticsearchDao.updateKeyInMultipleDocuments(
        entityType, keyToUpdate, newValue, filterKey, filterValue);
    assertThat(isSuccessful).isTrue();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testUpdateListInMultipleDocuments() {
    String elementKey = "element_key";
    String newValue = "new_value";
    when(elasticsearchDao.updateListInMultipleDocuments(entityType, listToUpdate, newValue, documentId, elementKey))
        .thenReturn(true);
    boolean isSuccessful = synchronousElasticsearchDao.updateListInMultipleDocuments(
        entityType, listToUpdate, newValue, documentId, elementKey);
    assertThat(isSuccessful).isTrue();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testAppendToListInSingleDocument() {
    when(elasticsearchDao.appendToListInSingleDocument(entityType, listToUpdate, documentId, newElement))
        .thenReturn(true);
    boolean isSuccessful =
        synchronousElasticsearchDao.appendToListInSingleDocument(entityType, listToUpdate, documentId, newElement);
    assertThat(isSuccessful).isTrue();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testAppendToListInSingleDocumentWithLimit() {
    when(elasticsearchDao.appendToListInSingleDocument(entityType, listToUpdate, documentId, newElement, limit))
        .thenReturn(true);
    boolean isSuccessful = synchronousElasticsearchDao.appendToListInSingleDocument(
        entityType, listToUpdate, documentId, newElement, limit);
    assertThat(isSuccessful).isTrue();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testAppendToListInMultipleDocuments() {
    when(elasticsearchDao.appendToListInMultipleDocuments(entityType, listToUpdate, documentIds, newElement))
        .thenReturn(true);
    boolean isSuccessful =
        synchronousElasticsearchDao.appendToListInMultipleDocuments(entityType, listToUpdate, documentIds, newElement);
    assertThat(isSuccessful).isTrue();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testAppendToListInMultipleDocumentsWithLimit() {
    when(elasticsearchDao.appendToListInMultipleDocuments(entityType, listToUpdate, documentIds, newElement, limit))
        .thenReturn(true);
    boolean isSuccessful = synchronousElasticsearchDao.appendToListInMultipleDocuments(
        entityType, listToUpdate, documentIds, newElement, limit);
    assertThat(isSuccessful).isTrue();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testRemoveFromListInMultipleDocuments() {
    String idToBeDeleted = generateUuid();
    when(elasticsearchDao.removeFromListInMultipleDocuments(entityType, listToUpdate, documentIds, idToBeDeleted))
        .thenReturn(true);
    boolean isSuccessful = synchronousElasticsearchDao.removeFromListInMultipleDocuments(
        entityType, listToUpdate, documentIds, idToBeDeleted);
    assertThat(isSuccessful).isTrue();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testRemoveFromListInMultipleDocument() {
    String idToBeDeleted = generateUuid();
    when(elasticsearchDao.removeFromListInMultipleDocuments(entityType, listToUpdate, idToBeDeleted)).thenReturn(true);
    boolean isSuccessful =
        synchronousElasticsearchDao.removeFromListInMultipleDocuments(entityType, listToUpdate, idToBeDeleted);
    assertThat(isSuccessful).isTrue();
  }
}
