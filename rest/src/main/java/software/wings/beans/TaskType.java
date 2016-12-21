package software.wings.beans;

import software.wings.api.HttpStateExecutionData;
import software.wings.delegatetasks.BambooCollectionTask;
import software.wings.delegatetasks.DelegateRunnableTask;
import software.wings.delegatetasks.HttpTask;
import software.wings.delegatetasks.JenkinsCollectionTask;
import software.wings.delegatetasks.JenkinsTask;
import software.wings.sm.states.JenkinsState.JenkinsExecutionResponse;
import software.wings.waitnotify.ListNotifyResponseData;
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
        String delegateId, DelegateTask delegateTask, Consumer<? extends NotifyResponseData> consumer) {
      return new HttpTask(delegateId, delegateTask, (Consumer<HttpStateExecutionData>) consumer);
    }
  },
  SPLUNK {
    private static final long serialVersionUID = 1L;

    @Override
    public DelegateRunnableTask getDelegateRunnableTask(
        String delegateId, DelegateTask delegateTask, Consumer<? extends NotifyResponseData> consumer) {
      return new HttpTask(delegateId, delegateTask, (Consumer<HttpStateExecutionData>) consumer);
    }
  },
  APP_DYNAMICS {
    private static final long serialVersionUID = 1L;

    @Override
    public DelegateRunnableTask getDelegateRunnableTask(
        String delegateId, DelegateTask delegateTask, Consumer<? extends NotifyResponseData> consumer) {
      return new HttpTask(delegateId, delegateTask, (Consumer<HttpStateExecutionData>) consumer);
    }
  },
  JENKINS {
    private static final long serialVersionUID = 1L;

    @Override
    public DelegateRunnableTask getDelegateRunnableTask(
        String delegateId, DelegateTask delegateTask, Consumer<? extends NotifyResponseData> consumer) {
      return new JenkinsTask(delegateId, delegateTask, (Consumer<JenkinsExecutionResponse>) consumer);
    }
  },
  JENKINS_COLLECTION {
    private static final long serialVersionUID = 1L;

    @Override
    public DelegateRunnableTask getDelegateRunnableTask(
        String delegateId, DelegateTask delegateTask, Consumer<? extends NotifyResponseData> consumer) {
      return new JenkinsCollectionTask(delegateId, delegateTask, (Consumer<ListNotifyResponseData>) consumer);
    }
  },
  BAMBOO_COLLECTION {
    private static final long serialVersionUID = 1L;

    @Override
    public DelegateRunnableTask getDelegateRunnableTask(
        String delegateId, DelegateTask delegateTask, Consumer<? extends NotifyResponseData> consumer) {
      return new BambooCollectionTask(delegateId, delegateTask, (Consumer<ListNotifyResponseData>) consumer);
    }
  };

  public abstract DelegateRunnableTask getDelegateRunnableTask(
      String delegateId, DelegateTask delegateTask, Consumer<? extends NotifyResponseData> consumer);
}
