package software.wings.delegatetasks.cv;

import software.wings.service.impl.analysis.DataCollectionInfoV2;

import java.lang.reflect.InvocationTargetException;

public class DataCollectorFactory {
  public <T extends DataCollector> DataCollector<? extends DataCollectionInfoV2> newInstance(Class<T> clazz)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    return clazz.getConstructor().newInstance();
  }
}
