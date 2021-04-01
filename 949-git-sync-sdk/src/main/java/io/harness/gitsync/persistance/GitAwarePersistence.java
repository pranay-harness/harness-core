package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.beans.NGDTO;

import com.mongodb.client.result.DeleteResult;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(DX)
public interface GitAwarePersistence<B extends GitSyncableEntity, Y extends NGDTO> {
  List<B> find(@NotNull Query query, String projectIdentifier, String orgIdentifier, String accountId);

  DeleteResult remove(@NotNull B object, Y yaml);

  B save(B objectToSave, Y yaml, ChangeType changeType);

  B save(B objectToSave, Y yaml);
}
