package software.wings.delegatetasks.azure;

import lombok.Getter;
import rx.CompletableSubscriber;
import rx.Subscription;

@Getter
public class DefaultCompletableSubscriber implements CompletableSubscriber {
  private CompletableSubscriberStatus status = CompletableSubscriberStatus.UNSUBSCRIBED;
  private Throwable error;
  private Subscription subscription;

  @Override
  public void onCompleted() {
    status = CompletableSubscriberStatus.COMPLETED;
    subscription.unsubscribe();
  }

  @Override
  public void onError(Throwable e) {
    status = CompletableSubscriberStatus.ERROR;
    this.error = e;
    subscription.unsubscribe();
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    status = CompletableSubscriberStatus.SUBSCRIBED;
    this.subscription = subscription;
  }

  public enum CompletableSubscriberStatus { UNSUBSCRIBED, SUBSCRIBED, ERROR, COMPLETED }
}
