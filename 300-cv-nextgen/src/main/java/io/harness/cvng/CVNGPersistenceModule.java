package io.harness.cvng;

import io.harness.notification.NotificationChannelPersistenceConfig;
import io.harness.serializer.CvNextGenRegistrars;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.springdata.SpringPersistenceModule;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.List;
import org.springframework.core.convert.converter.Converter;

public class CVNGPersistenceModule extends SpringPersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {SpringPersistenceConfig.class, NotificationChannelPersistenceConfig.class};
  }

  @Provides
  @Singleton
  List<Class<? extends Converter<?, ?>>> springConverters() {
    return ImmutableList.<Class<? extends Converter<?, ?>>>builder()
        .addAll(CvNextGenRegistrars.springConverters)
        .build();
  }
}
