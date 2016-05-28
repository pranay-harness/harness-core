package software.wings.service.intfc;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Log;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;

import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public interface LogService {
  PageResponse<Log> list(PageRequest<Log> pageRequest);

  @ValidationGroups(Create.class) Log save(@Valid Log log);
}
