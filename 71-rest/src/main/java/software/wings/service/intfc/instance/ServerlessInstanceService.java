package software.wings.service.intfc.instance;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.beans.infrastructure.instance.SyncStatus;

import java.util.Collection;
import java.util.List;
import javax.validation.Valid;

public interface ServerlessInstanceService {
  /**
   * Save instance information.
   *
   * @param instance the instance
   * @return the instance
   */
  @ValidationGroups(Create.class) ServerlessInstance save(@Valid ServerlessInstance instance);
  ServerlessInstance get(String instanceId);
  PageResponse<ServerlessInstance> list(PageRequest<ServerlessInstance> pageRequest);
  List<ServerlessInstance> list(String infraMappingId, String appId);
  boolean delete(Collection<String> instanceIds);
  ServerlessInstance update(ServerlessInstance instance);
  List<SyncStatus> getSyncStatus(String appId, String serviceId, String envId);
}
