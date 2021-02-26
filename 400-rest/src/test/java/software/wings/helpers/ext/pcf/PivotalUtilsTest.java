package software.wings.helpers.ext.pcf;

import static io.harness.rule.OwnerRule.ADWAIT;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.pcf.PcfUtils;
import io.harness.pcf.PivotalClientApiException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PcfUtils.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class PivotalUtilsTest extends WingsBaseTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCheckIfAppAutoscalarInstalled() throws Exception {
    PowerMockito.mockStatic(PcfUtils.class);
    ProcessExecutor processExecutor = mock(ProcessExecutor.class);
    ProcessResult processResult = mock(ProcessResult.class);
    doReturn(processResult).when(processExecutor).execute();
    doReturn("asd").doReturn(null).doReturn(EMPTY).when(processResult).outputUTF8();

    when(PcfUtils.createExecutorForAutoscalarPluginCheck(anyMap())).thenReturn(processExecutor);
    when(PcfUtils.checkIfAppAutoscalarInstalled()).thenCallRealMethod();
    assertThat(PcfUtils.checkIfAppAutoscalarInstalled()).isTrue(); // asd

    assertThat(PcfUtils.checkIfAppAutoscalarInstalled()).isFalse(); // null
    assertThat(PcfUtils.checkIfAppAutoscalarInstalled()).isFalse(); // empty

    doThrow(Exception.class).when(processExecutor).execute();
    try {
      PcfUtils.checkIfAppAutoscalarInstalled();
    } catch (Exception e) {
      assertThat(e instanceof PivotalClientApiException).isTrue();
    }
  }
}
