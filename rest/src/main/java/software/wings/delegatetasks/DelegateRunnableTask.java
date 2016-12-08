package software.wings.delegatetasks;

import software.wings.waitnotify.NotifyResponseData;

/**
 * Created by peeyushaggarwal on 12/7/16.
 */
public interface DelegateRunnableTask<T extends NotifyResponseData> extends Runnable { T run(Object[] parameters); }
