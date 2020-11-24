package io.harness.repositories.gitFileActivitySummary;

import com.mongodb.client.result.DeleteResult;
import java.util.List;

public interface GitFileActivitySummaryRepositoryCustom {
  DeleteResult deleteByIds(List<String> ids);
}
