package io.harness.distribution.barrier;

import static io.harness.distribution.barrier.Barrier.State.STANDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.distribution.barrier.Barrier.State;
import org.junit.Test;

public class BarrierTest {
  BarrierId id = new BarrierId("foo");
  ForcerId topId = new ForcerId("top");

  @Test
  public void testCreateBarrier() throws UnableToSaveBarrierException {
    BarrierRegistry registry = new InprocBarrierRegistry();

    Forcer forcer = Forcer.builder().build();

    Barrier barrier = Barrier.create(id, forcer, registry);
    assertThat(barrier).isNotNull();
    assertThat(barrier.getId()).isEqualTo(id);
    assertThat(barrier.getForcer()).isEqualTo(forcer);
  }

  @Test
  public void testLoadBarrier() throws UnableToSaveBarrierException, UnableToLoadBarrierException {
    BarrierRegistry registry = new InprocBarrierRegistry();

    Forcer forcer = Forcer.builder().build();

    Barrier.create(id, forcer, registry);
    Barrier barrier = Barrier.load(id, registry);

    assertThat(barrier).isNotNull();
    assertThat(barrier.getId()).isEqualTo(id);
    assertThat(barrier.getForcer()).isEqualTo(forcer);
  }

  @Test
  public void testSanityBreakBarrier() throws UnableToSaveBarrierException, UnableToLoadBarrierException {
    BarrierRegistry registry = new InprocBarrierRegistry();
    ForceProctor proctor = mock(ForceProctor.class);

    Forcer forcer = Forcer.builder().id(topId).build();

    Barrier barrier = Barrier.create(id, forcer, registry);

    when(proctor.getForcerState(topId)).thenReturn(Forcer.State.RUNNING);
    final State state = barrier.pushDown(proctor);

    assertThat(state).isEqualTo(STANDS);
  }
}
