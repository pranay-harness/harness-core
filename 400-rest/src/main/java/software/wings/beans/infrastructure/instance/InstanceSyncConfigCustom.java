package software.wings.beans.infrastructure.instance;

import com.mongodb.client.result.DeleteResult;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

public interface InstanceSyncConfigCustom {
    InstanceSyncConfig find(Criteria criteria);

    InstanceSyncConfig modify(Criteria criteria, Update update);

    InstanceSyncConfig add(InstanceSyncConfig instanceSyncConfig);

    DeleteResult delete(Criteria criteria);
}
