package software.wings.yaml.gitSync;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testShouldPerformStuckJobCheck() throws IllegalAccessException {
    assertThat(gitChangeSetRunnable.shouldPerformStuckJobCheck()).isTrue();

    setInternalState(gitChangeSetRunnable, "lastTimestampForStuckJobCheck",
        new AtomicLong(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(120)));
    assertThat(gitChangeSetRunnable.shouldPerformStuckJobCheck()).isTrue();

    FieldUtils.writeField(
        gitChangeSetRunnable, "lastTimestampForStuckJobCheck", new AtomicLong(System.currentTimeMillis()), true);
    assertThat(gitChangeSetRunnable.shouldPerformStuckJobCheck()).isFalse();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
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
    assertThat(stuckChangeSetIds).isNotNull();
    assertThat(stuckChangeSetIds).hasSize(1);
    assertThat(stuckChangeSetIds.get(0)).isEqualTo("12345");
  }
}
