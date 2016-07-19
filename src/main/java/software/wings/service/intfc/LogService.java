package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.Log;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;

import java.io.File;
import javax.validation.Valid;

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
  ExecutionResult getUnitExecutionResult(@NotEmpty String appId, @NotEmpty String activityId, @NotEmpty String name);

  /**
   * Export logs file.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @return the file
   */
  File exportLogs(@NotEmpty String appId, @NotEmpty String activityId);
}
