package io.harness;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.CDCEntity;

import com.google.inject.Inject;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CE)
@Slf4j
public class ChangeDataCaptureBulkSyncTask {
  @Inject private ChangeDataCaptureBulkMigrationHelper changeDataCaptureBulkMigrationHelper;
  @Inject private Set<CDCEntity<?>> cdcEntities;

  public boolean run() {
    return changeDataCaptureBulkMigrationHelper.doBulkSync(cdcEntities);
  }
}
