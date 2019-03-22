package software.wings.beans;

import io.harness.beans.EmbeddedUser;
import io.harness.persistence.CreatedByAccess;
import io.harness.persistence.UpdatedByAccess;

import java.util.ArrayList;
import java.util.List;

public interface KeywordsAware {
  List<String> getKeywords();
  void setKeywords(List<String> keywords);

  default List<String> generateKeywords() {
    List<String> keyWordList = new ArrayList<>();

    if (this instanceof CreatedByAccess) {
      CreatedByAccess createdByAccess = (CreatedByAccess) this;
      final EmbeddedUser createdBy = createdByAccess.getCreatedBy();
      if (createdBy != null) {
        keyWordList.add(createdBy.getName());
        keyWordList.add(createdBy.getEmail());
      }
    }

    if (this instanceof UpdatedByAccess) {
      UpdatedByAccess updatedByAccess = (UpdatedByAccess) this;
      final EmbeddedUser updatedBy = updatedByAccess.getLastUpdatedBy();
      if (updatedBy != null) {
        keyWordList.add(updatedBy.getName());
        keyWordList.add(updatedBy.getEmail());
      }
    }

    return keyWordList;
  }
}
