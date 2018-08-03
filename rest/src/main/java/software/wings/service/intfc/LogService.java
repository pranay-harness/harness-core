package software.wings.service.intfc;

import io.harness.validation.Create;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Log;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByActivity;

import java.io.File;
import java.util.List;
import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public interface LogService extends OwnedByActivity {
  /**
   * List.
   *
   * @param pageRequest the page request  @return the page response
   * @return the page response
   */
  PageResponse<Log> list(PageRequest<Log> pageRequest);

  /**
   * Save.
   *
   * @param log the log
   * @return the log
   */
  @ValidationGroups(Create.class) void save(@Valid Log log);

  /**
   * Gets unit execution result.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @param name       the name
   * @return the unit execution result
   */
  CommandExecutionStatus getUnitExecutionResult(
      @NotEmpty String appId, @NotEmpty String activityId, @NotEmpty String name);

  /**
   * Export logs file.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @return the file
   */
  File exportLogs(@NotEmpty String appId, @NotEmpty String activityId);

  /**
   * Batched save.
   *
   * @param logs the logs
   */
  void batchedSave(@Valid List<Log> logs);

  /**
   * Batched save command unit logs.
   *  @param activityId the activity id
   * @param unitName   the unit name
   * @param logs       the logs
   */
  String batchedSaveCommandUnitLogs(@NotEmpty String activityId, @NotEmpty String unitName, @Valid Log logs);
}
