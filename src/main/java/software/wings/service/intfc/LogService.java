package software.wings.service.intfc;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.Log;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;

import javax.validation.Valid;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public interface LogService {
  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Log> list(PageRequest<Log> pageRequest);

  /**
   * Save.
   *
   * @param log the log
   * @return the log
   */
  @ValidationGroups(Create.class) Log save(@Valid Log log);

  /**
   * Gets unit execution result.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @param name       the name
   * @return the unit execution result
   */
  ExecutionResult getUnitExecutionResult(String appId, String activityId, String name);
}
