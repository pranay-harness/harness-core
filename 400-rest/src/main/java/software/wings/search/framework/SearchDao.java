/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Map;

/**
 * The search DAO interface that has
 * to be implemented for CRUD operations.
 *
 * @author ujjawal
 */
@OwnedBy(PL)
public interface SearchDao {
  // Upserts the partial document to Elasticsearch
  boolean upsertDocument(String entityType, String entityId, String entityJson);

  // Updates key field with newValue in Elasticsearch Document
  boolean updateKeyInMultipleDocuments(
      String entityType, String keyToUpdate, String newValue, String filterKey, String filterValue);

  // Updates listToUpdate for documents in entityType(Index) with documentId
  boolean updateListInMultipleDocuments(
      String entityType, String listToUpdate, String newElement, String documentId, String elementKeyToChange);

  // Append newElement in the all the documentIds with listToUpdate field in entityType index
  boolean appendToListInMultipleDocuments(
      String entityType, String listToUpdate, List<String> documentIds, Map<String, Object> newElement);

  boolean appendToListInMultipleDocuments(String entityType, String listToUpdate, List<String> documentIds,
      Map<String, Object> newElement, int maxElementsInList);

  // Append newElement in the all the documentId with listToUpdate field in entityType index
  boolean appendToListInSingleDocument(
      String entityType, String listToUpdate, String documentId, Map<String, Object> newElement);

  // Append newElement in the all the documentId with listToUpdate field in entityType index and set the length to be
  // maxElementsInList
  boolean appendToListInSingleDocument(
      String entityType, String listToUpdate, String documentId, Map<String, Object> newElement, int maxElementsInList);

  // Remove documents(documentIds) in listToUpdate field and having id(idToBeDeleted) in index entityType.
  boolean removeFromListInMultipleDocuments(
      String entityType, String listToUpdate, List<String> documentIds, String idToBeDeleted);

  // Remove document(documentId) in listToUpdate field and having id(idToBeDeleted) in index entityType.
  boolean removeFromListInMultipleDocuments(
      String entityType, String listToUpdate, String documentId, String idToBeDeleted);

  // Remove idToBeDeleted from listToUpdate field in entityType index.
  boolean removeFromListInMultipleDocuments(String entityType, String listToUpdate, String idToBeDeleted);

  // Fires nested query on fieldName with given value in entityType index
  List<String> nestedQuery(String entityType, String fieldName, String value);

  // Add timestamp in the document also removing stale data wrt daysToRetain
  boolean addTimestamp(String entityType, String fieldName, String documentId, long createdAt, int daysToRetain);

  // Add timestamp to multiple documents and also removing stale data wrt daysToRetain
  boolean addTimestamp(String entityType, String fieldName, List<String> documentIds, long createdAt, int daysToRetain);

  // Delete document with id documentId in index
  boolean deleteDocument(String entityType, String documentId);
}
