package software.wings.beans.infrastructure.instance;

import com.mongodb.client.result.DeleteResult;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.CrudRepository;

public interface InstanceSyncConfigRepository extends CrudRepository<InstanceSyncConfig, String> ,InstanceSyncConfigCustom{
    InstanceSyncConfig findByAccountIdAndPerpetualTaskType(String accountId, String perpetualTaskType);

    void deleteByAccountIdAndPerpetualTaskType(String accountId,String perpetualTaskType);
}
