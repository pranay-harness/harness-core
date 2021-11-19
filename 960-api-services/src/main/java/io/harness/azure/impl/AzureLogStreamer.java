package io.harness.azure.impl;

import static io.harness.logging.LogLevel.INFO;

import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.utility.AzureLogParser;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.LogCallback;

import java.util.Optional;
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
  private DateTime startTime;

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
      subscription.unsubscribe();
    }

    if (logParser.checkIfFailed(log, containerDeployment)) {
      subscription.unsubscribe();
      throw new InvalidRequestException(log);
    }
  }

  public boolean isNewLog(String log) {
    Optional<DateTime> logTime = logParser.parseTime(log);
    return logTime.map(dateTime -> startTime.isBefore(dateTime)).orElse(false);
  }

  @Override
  public void run() {
    Observable<String> logStreamObservable = azureWebClient.streamDeploymentLogsAsync(azureWebClientContext, slotName);
    Subscriber<String> streamSubscriber = new Subscriber<String>() {
      @Override
      public void onCompleted() {
        unsubscribe();
      }

      @Override
      public void onError(Throwable e) {
        unsubscribe();
      }

      @Override
      public void onNext(String s) {
        streamLogs(s);
      }
    };

    logStreamObservable.subscribe(streamSubscriber);
    subscription = streamSubscriber;
  }
}