package software.wings.delegatetasks;

import static junit.framework.TestCase.fail;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.sm.states.APMStateVerificationTestBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class DataCollectionExecutorServiceTest extends APMStateVerificationTestBase {
  @Inject private DataCollectionExecutorService executorService;

  @Test(expected = IOException.class)
  public void executeParallelWithException() throws IOException {
    List<Callable<Boolean>> callables = new ArrayList<>();
    callables.add(() -> true);
    callables.add(() -> { throw new RuntimeException("fail on purpose"); });
    executorService.executeParrallel(callables);
  }

  @Test
  public void executeParallel() throws IOException {
    List<Callable<Boolean>> callables = new ArrayList<>();
    callables.add(() -> true);
    callables.add(() -> { throw new RuntimeException("fail on purpose"); });
    try {
      executorService.executeParrallel(callables);
      fail();
    } catch (IOException ignore) {
      // do nothing
    }
    callables.remove(1);
    executorService.executeParrallel(callables);
  }
}
