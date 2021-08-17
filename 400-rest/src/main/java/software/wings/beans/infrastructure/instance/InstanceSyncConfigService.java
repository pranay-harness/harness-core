package software.wings.beans.infrastructure.instance;

public interface InstanceSyncConfigService {
    InstanceSyncConfig save(String accountId,String perpetualTaskType,int timeInterval);

    InstanceSyncConfig getByAccountIdAndPerpetualTaskType(String accountId,String perpetualTaskType);

    boolean deleteByAccountIdAndPerpetualTaskType(String accountId,String perpetualTaskType);


}
