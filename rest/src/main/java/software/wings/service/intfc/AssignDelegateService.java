package software.wings.service.intfc;

import software.wings.beans.DelegateTask;

/**
 * Created by brett on 7/20/17
 */
public interface AssignDelegateService { boolean canAssign(DelegateTask task, String delegateId); }
