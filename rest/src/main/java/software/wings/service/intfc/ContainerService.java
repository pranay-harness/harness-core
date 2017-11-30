package software.wings.service.intfc;

import software.wings.api.ContainerServiceElement;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public interface ContainerService {
  @DelegateTaskType(TaskType.CONTAINER_SERVICE_DESIRED_COUNT)
  Optional<Integer> getServiceDesiredCount(SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, ContainerServiceElement containerServiceElement, String region);

  @DelegateTaskType(TaskType.CONTAINER_ACTIVE_SERVICE_COUNTS)
  LinkedHashMap<String, Integer> getActiveServiceCounts(SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, ContainerServiceElement containerServiceElement, String region);
}
