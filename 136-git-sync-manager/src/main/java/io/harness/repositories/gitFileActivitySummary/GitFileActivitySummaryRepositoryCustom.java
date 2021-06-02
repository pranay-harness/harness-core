package io.harness.repositories.gitFileActivitySummary;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.mongodb.client.result.DeleteResult;
import java.util.List;

@OwnedBy(DX)
public interface GitFileActivitySummaryRepositoryCustom {
  DeleteResult deleteByIds(List<String> ids);
}
