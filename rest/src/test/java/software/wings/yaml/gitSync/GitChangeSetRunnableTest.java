package software.wings.yaml.gitSync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.utils.WingsTestConstants;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class GitChangeSetRunnableTest extends WingsBaseTest {
  @Mock GitChangeSetRunnableHelper gitChangeSetRunnableHelper;
  @Mock YamlChangeSetService yamlChangeSetService;
  @Inject @InjectMocks private GitChangeSetRunnable gitChangeSetRunnable;

  @Test
  public void testShouldPerformStuckJobCheck() {
    assertTrue(gitChangeSetRunnable.shouldPerformStuckJobCheck());

    setInternalState(gitChangeSetRunnable, "lastTimestampForStuckJobCheck",
        new AtomicLong(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(120)));
    assertTrue(gitChangeSetRunnable.shouldPerformStuckJobCheck());

    setInternalState(gitChangeSetRunnable, "lastTimestampForStuckJobCheck", new AtomicLong(System.currentTimeMillis()));
    assertFalse(gitChangeSetRunnable.shouldPerformStuckJobCheck());
  }

  @Test
  public void testRetryAnyStuckYamlChangeSet() {
    YamlChangeSet yamlChangeSet = YamlChangeSet.builder().accountId(WingsTestConstants.ACCOUNT_ID).build();
    yamlChangeSet.setUuid("12345");

    doReturn(Arrays.asList(yamlChangeSet)).when(gitChangeSetRunnableHelper).getStuckYamlChangeSets(any(), anyList());
    doReturn(true)
        .when(yamlChangeSetService)
        .updateStatusForGivenYamlChangeSets(anyString(), any(), anyList(), anyList());

    gitChangeSetRunnable.retryAnyStuckYamlChangeSet(Arrays.asList("12345"));
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(yamlChangeSetService).updateStatusForGivenYamlChangeSets(anyString(), any(), anyList(), captor.capture());
    List stuckChangeSetIds = captor.getValue();
    assertNotNull(stuckChangeSetIds);
    assertEquals(1, stuckChangeSetIds.size());
    assertEquals("12345", stuckChangeSetIds.get(0));
  }
}
