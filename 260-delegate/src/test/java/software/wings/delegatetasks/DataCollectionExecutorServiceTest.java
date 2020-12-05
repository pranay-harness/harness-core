package software.wings.delegatetasks;

import static io.harness.rule.OwnerRule.SRIRAM;

import static junit.framework.TestCase.fail;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.DataCollectionExecutorService;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import software.wings.sm.states.APMStateVerificationTestBase;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(Module._930_DELEGATE_TASKS)
public class DataCollectionExecutorServiceTest extends APMStateVerificationTestBase {
  @Inject private DataCollectionExecutorService executorService;

  @Test(expected = WingsException.class)
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void executeParallelWithException() throws IOException {
    List<Callable<Boolean>> callables = new ArrayList<>();
    callables.add(() -> true);
    callables.add(() -> { throw new RuntimeException("fail on purpose"); });
    executorService.executeParrallel(callables);
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void executeParallel() {
    List<Callable<Boolean>> callables = new ArrayList<>();
    callables.add(() -> true);
    callables.add(() -> { throw new RuntimeException("fail on purpose"); });
    try {
      executorService.executeParrallel(callables);
      fail();
    } catch (WingsException ex) {
      // do nothing
    }
    callables.remove(1);
    executorService.executeParrallel(callables);
  }
}
