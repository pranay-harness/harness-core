package software.wings.delegatetasks.cv;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.analysis.DataCollectionInfoV2;

import java.lang.reflect.InvocationTargetException;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class DataCollectorFactory {
  public <T extends DataCollector> DataCollector<? extends DataCollectionInfoV2> newInstance(Class<T> clazz)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    return clazz.getConstructor().newInstance();
  }
}
