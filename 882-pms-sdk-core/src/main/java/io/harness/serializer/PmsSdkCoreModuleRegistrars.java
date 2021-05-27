package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.ImmutableList;
import lombok.experimental.UtilityClass;
import org.springframework.core.convert.converter.Converter;

@OwnedBy(CDC)
@UtilityClass
public class PmsSdkCoreModuleRegistrars {
  public final ImmutableList<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.<Class<? extends Converter<?, ?>>>builder()
          .addAll(PmsCommonsModuleRegistrars.springConverters)
          .build();
}
