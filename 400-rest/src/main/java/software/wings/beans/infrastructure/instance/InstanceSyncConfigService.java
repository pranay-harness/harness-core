package software.wings.beans.infrastructure.instance;

public interface InstanceSyncConfigService {
    PerpetualTaskScheduleConfig save(String accountId, String perpetualTaskType, int timeInterval);

    PerpetualTaskScheduleConfig getByAccountIdAndPerpetualTaskType(String accountId, String perpetualTaskType);

    boolean deleteByAccountIdAndPerpetualTaskType(String accountId,String perpetualTaskType);


}
