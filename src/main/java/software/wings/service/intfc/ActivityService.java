package software.wings.service.intfc;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Activity;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;

import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public interface ActivityService {
  PageResponse<Activity> list(PageRequest<Activity> pageRequest);

  Activity get(String id, String appId);

  @ValidationGroups(Create.class) Activity save(@Valid Activity activity);
}
