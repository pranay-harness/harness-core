package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.INFO;

import static java.lang.String.format;

import software.wings.beans.command.ExecutionLogCallback;

import com.microsoft.rest.ServiceCallback;
import java.util.concurrent.atomic.AtomicBoolean;

public class AzureRestCallBack<T> implements ServiceCallback<T> {
  private Throwable throwable;
  private final AtomicBoolean updateFailed = new AtomicBoolean();
  private final ExecutionLogCallback logCallBack;
  private final String resourceName;

  public AzureRestCallBack(ExecutionLogCallback logCallBack, String resourceName) {
    this.logCallBack = logCallBack;
    this.resourceName = resourceName;
  }

  @Override
  public void failure(Throwable t) {
    throwable = t;
    updateFailed.set(true);
  }

  @Override
  public void success(T result) {
    logCallBack.saveExecutionLog(
        format("Received success response from Azure for VMSS: [%s] update capacity", resourceName), INFO, SUCCESS);
  }

  public boolean updateFailed() {
    return updateFailed.get();
  }

  public String failureMessage() {
    return throwable.getMessage();
  }
}
