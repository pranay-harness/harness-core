package io.harness.serializer;

import io.harness.delegate.serializer.DelegateTasksRegistrars;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.OrchestrationKryoRegister;
import io.harness.serializer.morphia.OrchestrationMorphiaRegistrar;
import io.harness.serializer.morphia.converters.AdviserObtainmentMorphiaConverter;
import io.harness.serializer.morphia.converters.AdviserTypeMorphiaConverter;
import io.harness.serializer.morphia.converters.AmbianceMorphiaConverter;
import io.harness.serializer.morphia.converters.ExecutableResponseMorphiaConverter;
import io.harness.serializer.morphia.converters.ExecutionMetadataMorphiaConverter;
import io.harness.serializer.morphia.converters.ExecutionTriggerInfoMorphiaConverter;
import io.harness.serializer.morphia.converters.FacilitatorObtainmentMorphiaConverter;
import io.harness.serializer.morphia.converters.FacilitatorTypeMorphiaConverter;
import io.harness.serializer.morphia.converters.FailureInfoMorphiaConverter;
import io.harness.serializer.morphia.converters.LevelMorphiaConverter;
import io.harness.serializer.morphia.converters.RefObjectMorphiaConverter;
import io.harness.serializer.morphia.converters.RefTypeMorphiaConverter;
import io.harness.serializer.morphia.converters.StepTypeMorphiaConverter;
import io.harness.serializer.morphia.converters.TriggeredByMorphiaConverter;
import io.harness.serializer.spring.converters.advisers.obtainment.AdviserObtainmentReadConverter;
import io.harness.serializer.spring.converters.advisers.obtainment.AdviserObtainmentWriteConverter;
import io.harness.serializer.spring.converters.advisers.type.AdviserTypeReadConverter;
import io.harness.serializer.spring.converters.advisers.type.AdviserTypeWriteConverter;
import io.harness.serializer.spring.converters.ambiance.AmbianceReadConverter;
import io.harness.serializer.spring.converters.ambiance.AmbianceWriteConverter;
import io.harness.serializer.spring.converters.errorInfo.ExecutionErrorInfoReadConverter;
import io.harness.serializer.spring.converters.errorInfo.ExecutionErrorInfoWriteConverter;
import io.harness.serializer.spring.converters.executableresponse.ExecutableResponseReadConverter;
import io.harness.serializer.spring.converters.executableresponse.ExecutableResponseWriteConverter;
import io.harness.serializer.spring.converters.executionmetadata.ExecutionMetadataReadConverter;
import io.harness.serializer.spring.converters.executionmetadata.ExecutionMetadataWriteConverter;
import io.harness.serializer.spring.converters.facilitators.obtainment.FacilitatorObtainmentReadConverter;
import io.harness.serializer.spring.converters.facilitators.obtainment.FacilitatorObtainmentWriteConverter;
import io.harness.serializer.spring.converters.facilitators.response.FacilitatorResponseReadConverter;
import io.harness.serializer.spring.converters.facilitators.response.FacilitatorResponseWriteConverter;
import io.harness.serializer.spring.converters.facilitators.type.FacilitatorTypeReadConverter;
import io.harness.serializer.spring.converters.facilitators.type.FacilitatorTypeWriteConverter;
import io.harness.serializer.spring.converters.failureinfo.FailureInfoReadConverter;
import io.harness.serializer.spring.converters.failureinfo.FailureInfoWriteConverter;
import io.harness.serializer.spring.converters.graphlayout.GraphLayoutNodeWriteConverter;
import io.harness.serializer.spring.converters.graphlayout.GraphLayoutReadConverter;
import io.harness.serializer.spring.converters.graphlayout.LayoutNodeInfoReadConverter;
import io.harness.serializer.spring.converters.graphlayout.LayoutNodeInfoWriteConverter;
import io.harness.serializer.spring.converters.level.LevelReadConverter;
import io.harness.serializer.spring.converters.level.LevelWriteConverter;
import io.harness.serializer.spring.converters.nodeexecution.NodeExecutionReadConverter;
import io.harness.serializer.spring.converters.nodeexecution.NodeExecutionWriteConverter;
import io.harness.serializer.spring.converters.plannode.PlanNodeProtoReadConverter;
import io.harness.serializer.spring.converters.plannode.PlanNodeProtoWriteConverter;
import io.harness.serializer.spring.converters.refobject.RefObjectReadConverter;
import io.harness.serializer.spring.converters.refobject.RefObjectWriteConverter;
import io.harness.serializer.spring.converters.reftype.RefTypeReadConverter;
import io.harness.serializer.spring.converters.reftype.RefTypeWriteConverter;
import io.harness.serializer.spring.converters.stepoutcomeref.StepOutcomeRefReadConverter;
import io.harness.serializer.spring.converters.stepoutcomeref.StepOutcomeRefWriteConverter;
import io.harness.serializer.spring.converters.steps.StepInfoReadConverter;
import io.harness.serializer.spring.converters.steps.StepInfoWriteConverter;
import io.harness.serializer.spring.converters.steps.StepTypeReadConverter;
import io.harness.serializer.spring.converters.steps.StepTypeWriteConverter;
import io.harness.serializer.spring.converters.sweepingoutput.SweepingOutputReadMongoConverter;
import io.harness.serializer.spring.converters.sweepingoutput.SweepingOutputWriteMongoConverter;
import io.harness.serializer.spring.converters.triggers.ExecutionTriggerInfoReadConverter;
import io.harness.serializer.spring.converters.triggers.ExecutionTriggerInfoWriteConverter;
import io.harness.serializer.spring.converters.triggers.TriggeredByReadConverter;
import io.harness.serializer.spring.converters.triggers.TriggeredByWriteConverter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
public class OrchestrationRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(DelegateTasksRegistrars.kryoRegistrars)
          .addAll(WaitEngineRegistrars.kryoRegistrars)
          .addAll(OrchestrationBeansRegistrars.kryoRegistrars)
          .add(OrchestrationKryoRegister.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(DelegateTasksRegistrars.morphiaRegistrars)
          .addAll(WaitEngineRegistrars.morphiaRegistrars)
          .addAll(DelegateServiceDriverRegistrars.morphiaRegistrars)
          .addAll(OrchestrationBeansRegistrars.morphiaRegistrars)
          .add(OrchestrationMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder()
          .addAll(OrchestrationBeansRegistrars.morphiaConverters)
          .add(AmbianceMorphiaConverter.class)
          .add(LevelMorphiaConverter.class)
          .add(StepTypeMorphiaConverter.class)
          .add(RefObjectMorphiaConverter.class)
          .add(AdviserTypeMorphiaConverter.class)
          .add(AdviserObtainmentMorphiaConverter.class)
          .add(FacilitatorTypeMorphiaConverter.class)
          .add(FacilitatorObtainmentMorphiaConverter.class)
          .add(RefTypeMorphiaConverter.class)
          .add(FailureInfoMorphiaConverter.class)
          .add(ExecutableResponseMorphiaConverter.class)
          .add(ExecutionTriggerInfoMorphiaConverter.class)
          .add(TriggeredByMorphiaConverter.class)
          .add(ExecutionMetadataMorphiaConverter.class)
          .build();

  public static final List<Class<? extends Converter<?, ?>>> springConverters = ImmutableList.of(
      SweepingOutputReadMongoConverter.class, SweepingOutputWriteMongoConverter.class, AmbianceReadConverter.class,
      AmbianceWriteConverter.class, LevelReadConverter.class, LevelWriteConverter.class, AdviserTypeReadConverter.class,
      AdviserTypeWriteConverter.class, AdviserObtainmentReadConverter.class, AdviserObtainmentWriteConverter.class,
      FacilitatorTypeReadConverter.class, FacilitatorTypeWriteConverter.class, StepTypeReadConverter.class,
      StepTypeWriteConverter.class, FacilitatorObtainmentReadConverter.class, FacilitatorObtainmentWriteConverter.class,
      RefTypeReadConverter.class, RefTypeWriteConverter.class, RefObjectReadConverter.class,
      RefObjectWriteConverter.class, StepInfoReadConverter.class, StepInfoWriteConverter.class,
      FailureInfoReadConverter.class, FailureInfoWriteConverter.class, PlanNodeProtoReadConverter.class,
      PlanNodeProtoWriteConverter.class, StepOutcomeRefReadConverter.class, StepOutcomeRefWriteConverter.class,
      ExecutableResponseReadConverter.class, ExecutableResponseWriteConverter.class, NodeExecutionReadConverter.class,
      NodeExecutionWriteConverter.class, FacilitatorResponseReadConverter.class,
      FacilitatorResponseWriteConverter.class, GraphLayoutReadConverter.class, GraphLayoutNodeWriteConverter.class,
      ExecutionErrorInfoReadConverter.class, ExecutionErrorInfoWriteConverter.class, LayoutNodeInfoReadConverter.class,
      LayoutNodeInfoWriteConverter.class, ExecutionTriggerInfoReadConverter.class,
      ExecutionTriggerInfoWriteConverter.class, TriggeredByReadConverter.class, TriggeredByWriteConverter.class,
      ExecutionMetadataReadConverter.class, ExecutionMetadataWriteConverter.class);
}
