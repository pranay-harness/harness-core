/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.azure.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;

import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.utility.AzureLogParser;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.LogCallback;

import com.microsoft.azure.CloudException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

public class AzureLogStreamer implements Runnable {
  AzureWebClientContext azureWebClientContext;
  AzureWebClient azureWebClient;
  String slotName;
  LogCallback logCallback;
  AzureLogParser logParser;
  boolean containerDeployment;
  private Subscription subscription;
  private final DateTime startTime;
  private final AtomicBoolean operationCompleted = new AtomicBoolean();

  public AzureLogStreamer(AzureWebClientContext azureWebClientContext, AzureWebClient azureWebClient, String slotName,
      LogCallback logCallback, boolean containerDeployment) {
    this.azureWebClientContext = azureWebClientContext;
    this.azureWebClient = azureWebClient;
    this.slotName = slotName;
    this.logCallback = logCallback;
    this.logParser = new AzureLogParser();
    this.containerDeployment = containerDeployment;
    this.startTime = new DateTime(DateTimeZone.UTC);
  }

  private void validateAndLog(String log) {
    if (!logParser.shouldLog(log)) {
      return;
    }
    logCallback.saveExecutionLog(log, INFO);
  }

  protected void streamLogs(String s) {
    if (!isNewLog(s)) {
      return;
    }
    String log = logParser.removeTimestamp(s);
    validateAndLog(log);
    if (logParser.checkIsSuccessDeployment(log, containerDeployment)) {
      operationCompleted.set(true);
      logCallback.saveExecutionLog(String.format("Deployment on slot - [%s] was successful", slotName), INFO, SUCCESS);
      subscription.unsubscribe();
    }

    if (logParser.checkIfFailed(log, containerDeployment)) {
      operationCompleted.set(true);
      logCallback.saveExecutionLog(String.format("Deployment on slot - [%s] failed", slotName), ERROR, FAILURE);
      subscription.unsubscribe();
      throw new InvalidRequestException(log);
    }
  }

  public boolean isNewLog(String log) {
    Optional<DateTime> logTime = logParser.parseTime(log);
    return logTime.map(startTime::isBefore).orElse(false);
  }

  @Override
  public void run() {
    Observable<String> logStreamObservable = azureWebClient.streamDeploymentLogsAsync(azureWebClientContext, slotName);
    Subscriber<String> streamSubscriber = new Subscriber<String>() {
      @Override
      public void onCompleted() {
        operationCompleted.set(true);
        unsubscribe();
      }

      @Override
      public void onError(Throwable e) {
        if (!operationCompleted.get()) {
          String errorMessage = getErrorMessage(e);
          logCallback.saveExecutionLog(color(
              String.format(
                  "Failed to stream the deployment logs from slot - [%s] due to %n [%s]. %nPlease verify the status of deployment manually",
                  slotName, isEmpty(errorMessage) ? "" : errorMessage),
              White, Bold));
        }
        operationCompleted.set(true);
        unsubscribe();
      }

      @Override
      public void onNext(String s) {
        if (!operationCompleted.get()) {
          streamLogs(s);
        } else {
          unsubscribe();
        }
      }
    };
    subscription = streamSubscriber;
    logStreamObservable.subscribe(streamSubscriber);
  }

  public String getErrorMessage(Throwable e) {
    String failureMessage = failureMessage(e);
    String bodyMessage = getBodyMessage(e);
    if (bodyMessage == null) {
      return failureMessage;
    } else {
      return format("%s: %s", failureMessage, bodyMessage);
    }
  }

  private String failureMessage(Throwable throwable) {
    return throwable.getMessage();
  }

  private String getBodyMessage(Throwable throwable) {
    if (throwable instanceof CloudException) {
      CloudException cloudException = (CloudException) throwable;
      if (cloudException.body() != null) {
        return cloudException.body().message();
      }
    }
    return null;
  }
}
