package io.harness.distribution.barrier;

import static io.harness.distribution.barrier.Barrier.State.DOWN;
import static io.harness.distribution.barrier.Barrier.State.OUTLAST;
import static io.harness.distribution.barrier.Barrier.State.STANDS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import io.harness.distribution.barrier.Barrier.State;
import org.junit.Test;

public class BarrierTest {
  BarrierId id = new BarrierId("foo");
  ForcerId topId = new ForcerId("top");
  ForcerId child1Id = new ForcerId("child1");
  ForcerId child2Id = new ForcerId("child2");
  ForcerId child3Id = new ForcerId("child3");

  Forcer forcerTree = Forcer.builder()
                          .id(topId)
                          .children(asList(Forcer.builder().id(child1Id).build(), Forcer.builder().id(child2Id).build(),
                              Forcer.builder().id(child3Id).build()))
                          .build();

  @Test
  public void testCreateBarrier() throws UnableToSaveBarrierException {
    BarrierRegistry registry = new InprocBarrierRegistry();

    Barrier barrier = Barrier.create(id, forcerTree, registry);
    assertThat(barrier).isNotNull();
    assertThat(barrier.getId()).isEqualTo(id);
    assertThat(barrier.getForcer()).isEqualTo(forcerTree);
  }

  @Test
  public void testLoadBarrier() throws UnableToSaveBarrierException, UnableToLoadBarrierException {
    BarrierRegistry registry = new InprocBarrierRegistry();

    Barrier.create(id, forcerTree, registry);
    Barrier barrier = Barrier.load(id, registry);

    assertThat(barrier).isNotNull();
    assertThat(barrier.getId()).isEqualTo(id);
    assertThat(barrier.getForcer()).isEqualTo(forcerTree);
  }

  @Test
  public void testRunningForcer() throws UnableToSaveBarrierException, UnableToLoadBarrierException {
    BarrierRegistry registry = new InprocBarrierRegistry();
    ForceProctor proctor = mock(ForceProctor.class);

    Barrier barrier = Barrier.create(id, forcerTree, registry);

    when(proctor.getForcerState(topId)).thenReturn(Forcer.State.RUNNING);
    when(proctor.getForcerState(child1Id)).thenReturn(Forcer.State.RUNNING);
    when(proctor.getForcerState(child2Id)).thenReturn(Forcer.State.SUCCEEDED);
    when(proctor.getForcerState(child3Id)).thenReturn(Forcer.State.RUNNING);
    final State state = barrier.pushDown(proctor);

    assertThat(state).isEqualTo(STANDS);
  }

  @Test
  public void testChildFailedForcer() throws UnableToSaveBarrierException, UnableToLoadBarrierException {
    BarrierRegistry registry = new InprocBarrierRegistry();
    ForceProctor proctor = mock(ForceProctor.class);

    Barrier barrier = Barrier.create(id, forcerTree, registry);

    when(proctor.getForcerState(topId)).thenReturn(Forcer.State.RUNNING);
    when(proctor.getForcerState(child1Id)).thenReturn(Forcer.State.SUCCEEDED);
    when(proctor.getForcerState(child2Id)).thenReturn(Forcer.State.FAILED);
    when(proctor.getForcerState(child3Id)).thenReturn(Forcer.State.RUNNING);
    final State state = barrier.pushDown(proctor);

    assertThat(state).isEqualTo(OUTLAST);
  }

  @Test
  public void testChildMixedForcer() throws UnableToSaveBarrierException, UnableToLoadBarrierException {
    BarrierRegistry registry = new InprocBarrierRegistry();
    ForceProctor proctor = mock(ForceProctor.class);

    Barrier barrier = Barrier.create(id, forcerTree, registry);

    when(proctor.getForcerState(topId)).thenReturn(Forcer.State.RUNNING);
    when(proctor.getForcerState(child1Id)).thenReturn(Forcer.State.RUNNING);
    when(proctor.getForcerState(child2Id)).thenReturn(Forcer.State.SUCCEEDED);
    when(proctor.getForcerState(child3Id)).thenReturn(Forcer.State.RUNNING);
    final State state = barrier.pushDown(proctor);

    assertThat(state).isEqualTo(STANDS);
  }

  @Test
  public void testTopSucceededForcer() throws UnableToSaveBarrierException, UnableToLoadBarrierException {
    BarrierRegistry registry = new InprocBarrierRegistry();
    ForceProctor proctor = mock(ForceProctor.class);

    Barrier barrier = Barrier.create(id, forcerTree, registry);

    when(proctor.getForcerState(topId)).thenReturn(Forcer.State.SUCCEEDED);
    final State state = barrier.pushDown(proctor);

    verify(proctor, times(1)).getForcerState(any());

    assertThat(state).isEqualTo(DOWN);
  }

  @Test
  public void testFailedForcer() throws UnableToSaveBarrierException, UnableToLoadBarrierException {
    BarrierRegistry registry = new InprocBarrierRegistry();
    ForceProctor proctor = mock(ForceProctor.class);

    Barrier barrier = Barrier.create(id, forcerTree, registry);

    when(proctor.getForcerState(topId)).thenReturn(Forcer.State.FAILED);
    final State state = barrier.pushDown(proctor);

    verify(proctor, times(1)).getForcerState(any());

    assertThat(state).isEqualTo(OUTLAST);
  }
}
