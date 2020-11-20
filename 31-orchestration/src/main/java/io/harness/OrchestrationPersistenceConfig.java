package io.harness;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.orchestration.persistence.OrchestrationBasePersistenceConfig;
import io.harness.serializer.spring.converters.advisers.obtainment.AdviserObtainmentReadConverter;
import io.harness.serializer.spring.converters.advisers.obtainment.AdviserObtainmentWriteConverter;
import io.harness.serializer.spring.converters.advisers.type.AdviserTypeReadConverter;
import io.harness.serializer.spring.converters.advisers.type.AdviserTypeWriteConverter;
import io.harness.serializer.spring.converters.ambiance.AmbianceReadConverter;
import io.harness.serializer.spring.converters.ambiance.AmbianceWriteConverter;
import io.harness.serializer.spring.converters.facilitators.obtainment.FacilitatorObtainmentReadConverter;
import io.harness.serializer.spring.converters.facilitators.obtainment.FacilitatorObtainmentWriteConverter;
import io.harness.serializer.spring.converters.facilitators.type.FacilitatorTypeReadConverter;
import io.harness.serializer.spring.converters.facilitators.type.FacilitatorTypeWriteConverter;
import io.harness.serializer.spring.converters.level.LevelReadConverter;
import io.harness.serializer.spring.converters.level.LevelWriteConverter;
import io.harness.serializer.spring.converters.refobject.RefObjectReadConverter;
import io.harness.serializer.spring.converters.refobject.RefObjectWriteConverter;
import io.harness.serializer.spring.converters.reftype.RefTypeReadConverter;
import io.harness.serializer.spring.converters.reftype.RefTypeWriteConverter;
import io.harness.serializer.spring.converters.steps.StepTypeReadConverter;
import io.harness.serializer.spring.converters.steps.StepTypeWriteConverter;
import io.harness.serializer.spring.converters.sweepingoutput.SweepingOutputReadMongoConverter;
import io.harness.serializer.spring.converters.sweepingoutput.SweepingOutputWriteMongoConverter;
import io.harness.spring.AliasRegistrar;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.List;
import java.util.Set;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.engine"},
    includeFilters = @ComponentScan.Filter(HarnessRepo.class), mongoTemplateRef = "orchestrationMongoTemplate")
public class OrchestrationPersistenceConfig extends OrchestrationBasePersistenceConfig {
  private static final List<Class<? extends Converter>> converters = ImmutableList.of(
      SweepingOutputReadMongoConverter.class, SweepingOutputWriteMongoConverter.class, AmbianceReadConverter.class,
      AmbianceWriteConverter.class, LevelReadConverter.class, LevelWriteConverter.class, AdviserTypeReadConverter.class,
      AdviserTypeWriteConverter.class, AdviserObtainmentReadConverter.class, AdviserObtainmentWriteConverter.class,
      FacilitatorTypeReadConverter.class, FacilitatorTypeWriteConverter.class, StepTypeReadConverter.class,
      StepTypeWriteConverter.class, FacilitatorObtainmentReadConverter.class, FacilitatorObtainmentWriteConverter.class,
      RefTypeReadConverter.class, RefTypeWriteConverter.class, RefObjectReadConverter.class,
      RefObjectWriteConverter.class);
  @Inject
  public OrchestrationPersistenceConfig(Injector injector, Set<Class<? extends AliasRegistrar>> aliasRegistrars) {
    super(injector, aliasRegistrars, converters);
  }
}