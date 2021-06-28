package io.harness.ccm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.springdata.SpringPersistenceModule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;

@OwnedBy(HarnessTeam.CE)
public class CENextGenPersistenceModule extends SpringPersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    List<Class<?>> resultClasses = Lists.newArrayList(ImmutableList.of(SpringPersistenceConfig.class));
    Class<?>[] resultClassesArray = new Class<?>[ resultClasses.size() ];
    return resultClasses.toArray(resultClassesArray);
  }
}
