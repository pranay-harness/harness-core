package software.wings.beans.infrastructure.instance;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.MongoOperations;

@AllArgsConstructor(onConstructor = @__({ @Inject}))
public class InstanceSyncConfigCustomImpl implements InstanceSyncConfigCustom{
    private MongoTemplate mongoTemplate;

    @Override
    public InstanceSyncConfig find(Criteria criteria) {
        Query query=new Query(criteria);
        return mongoTemplate.findOne(query,InstanceSyncConfig.class);
    }

    public InstanceSyncConfig modify(Criteria criteria,Update update)
    {
        Query query=new Query(criteria);
        return mongoTemplate.findAndModify(query,update,InstanceSyncConfig.class);
    }

    public InstanceSyncConfig add(InstanceSyncConfig instanceSyncConfig)
    {
        return mongoTemplate.insert(instanceSyncConfig,"instanceSyncConfig");
    }
    public DeleteResult delete(Criteria criteria){
        Query query=new Query(criteria);
        return mongoTemplate.remove(query,InstanceSyncConfig.class);
    }
}
