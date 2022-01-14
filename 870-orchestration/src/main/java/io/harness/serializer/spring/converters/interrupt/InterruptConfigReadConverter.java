package io.harness.serializer.spring.converters.interrupt;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.serializer.spring.ProtoReadConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class InterruptConfigReadConverter extends ProtoReadConverter<InterruptConfig> {
  public InterruptConfigReadConverter() {
    super(InterruptConfig.class);
  }
}
