package io.harness.serializer.kryo;

import io.harness.engine.interrupts.steps.SimpleStepAsyncParams;
import io.harness.grpc.TestTransportEntity;
import io.harness.serializer.KryoRegistrar;

import software.wings.expression.ManagerExpressionEvaluatorTest;
import software.wings.service.impl.SweepingOutputServiceImplTest;
import software.wings.service.impl.WorkflowExecutionUpdateFake;
import software.wings.sm.StateMachineExecutionCallbackMock;
import software.wings.sm.StateMachineExecutorTest.CustomExecutionEventAdvisor;
import software.wings.sm.StateMachineTest.TestStateExecutionData;

import com.esotericsoftware.kryo.Kryo;

public class TestManagerKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    int index = 71 * 10000;
    kryo.register(StateMachineExecutionCallbackMock.class, index++);
    kryo.register(TestStateExecutionData.class, index++);
    kryo.register(CustomExecutionEventAdvisor.class, index++);
    kryo.register(WorkflowExecutionUpdateFake.class, index++);

    kryo.register(SweepingOutputServiceImplTest.SweepingOutputData.class, index++);
    kryo.register(ManagerExpressionEvaluatorTest.SweepingOutputData.class, index++);

    kryo.register(TestTransportEntity.class, index++);

    kryo.register(SimpleStepAsyncParams.class, index++);
  }
}
