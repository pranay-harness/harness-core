package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Graph;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.validation.Valid;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface ServiceResourceService {
  PageResponse<Service> list(PageRequest<Service> pageRequest);

  @ValidationGroups(Create.class) Service save(@Valid Service service);

  @ValidationGroups(Update.class) Service update(@Valid Service service);

  Service get(@NotEmpty String appId, @NotEmpty String serviceId);

  void delete(@NotEmpty String appId, @NotEmpty String serviceId);

  Service addCommand(@NotEmpty String appId, @NotEmpty String serviceId, Graph commandGraph);

  Service deleteCommand(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String commandName);

  List<Object> getCommandStencils(@NotEmpty String appId, @NotEmpty String serviceId);
}
