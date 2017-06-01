package software.wings.delegatetasks;

import software.wings.beans.Log;

import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 1/9/17.
 */
public interface DelegateLogService { void save(String accountId, @Valid Log log); }
