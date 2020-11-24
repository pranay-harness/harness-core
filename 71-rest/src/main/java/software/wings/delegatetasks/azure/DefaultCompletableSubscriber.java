package software.wings.delegatetasks.azure;

import lombok.Getter;
import rx.CompletableSubscriber;
import rx.Observer;
import rx.Subscription;

@Getter
public class DefaultCompletableSubscriber implements CompletableSubscriber, Observer<Void> {
  private CompletableSubscriberStatus status = CompletableSubscriberStatus.UNSUBSCRIBED;
  private Throwable error;
  private Subscription subscription;

  @Override
  public void onCompleted() {
    status = CompletableSubscriberStatus.COMPLETED;
    unsubscribe();
  }

  @Override
  public void onError(Throwable e) {
    status = CompletableSubscriberStatus.ERROR;
    this.error = e;
    unsubscribe();
  }

  private void unsubscribe() {
    if (subscription != null) {
      subscription.unsubscribe();
    }
  }

  @Override
  public void onNext(Void unused) {
    // this is completable subscriber where listening only for onCompleted and  onError events
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    status = CompletableSubscriberStatus.SUBSCRIBED;
    this.subscription = subscription;
  }

  public enum CompletableSubscriberStatus { UNSUBSCRIBED, SUBSCRIBED, ERROR, COMPLETED }
}
