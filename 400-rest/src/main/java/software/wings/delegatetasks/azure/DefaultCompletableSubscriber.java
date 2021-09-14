/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.azure;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Getter;
import rx.CompletableSubscriber;
import rx.Observer;
import rx.Subscription;

@Getter
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
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

  public void unsubscribe() {
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
