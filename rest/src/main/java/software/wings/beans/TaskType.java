package software.wings.beans;

import software.wings.api.HttpStateExecutionData;
import software.wings.delegatetasks.DelegateRunnableTask;
import software.wings.delegatetasks.HttpTask;
import software.wings.delegatetasks.JenkinsTask;
import software.wings.sm.states.JenkinsState.JenkinsExecutionResponse;
import software.wings.waitnotify.NotifyResponseData;

import java.util.function.Consumer;

/**
 * Created by peeyushaggarwal on 12/8/16.
 */
public enum TaskType {
  HTTP {
    private static final long serialVersionUID = 1L;

    @Override
    public DelegateRunnableTask getDelegateRunnableTask(
        String id, Object[] parameters, Consumer<? extends NotifyResponseData> consumer) {
      return new HttpTask(id, parameters, (Consumer<HttpStateExecutionData>) consumer);
    }
  },
  SPLUNK {
    private static final long serialVersionUID = 1L;

    @Override
    public DelegateRunnableTask getDelegateRunnableTask(
        String id, Object[] parameters, Consumer<? extends NotifyResponseData> consumer) {
      return new HttpTask(id, parameters, (Consumer<HttpStateExecutionData>) consumer);
    }
  },
  APP_DYNAMICS {
    private static final long serialVersionUID = 1L;

    @Override
    public DelegateRunnableTask getDelegateRunnableTask(
        String id, Object[] parameters, Consumer<? extends NotifyResponseData> consumer) {
      return new HttpTask(id, parameters, (Consumer<HttpStateExecutionData>) consumer);
    }
  },
  JENKINS {
    private static final long serialVersionUID = 1L;

    @Override
    public DelegateRunnableTask getDelegateRunnableTask(
        String id, Object[] parameters, Consumer<? extends NotifyResponseData> consumer) {
      return new JenkinsTask(id, parameters, (Consumer<JenkinsExecutionResponse>) consumer);
    }
  };

  public abstract DelegateRunnableTask getDelegateRunnableTask(
      String id, Object[] parameters, Consumer<? extends NotifyResponseData> consumer);
}
