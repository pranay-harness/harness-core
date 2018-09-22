package software.wings.service.impl.yaml.service;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static software.wings.beans.yaml.GitFileChange.Builder.aGitFileChange;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.yaml.YamlGitService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class YamlServiceImplTest extends WingsBaseTest {
  @Mock private YamlHandlerFactory yamlHandlerFactory;
  @Mock private YamlHelper yamlHelper;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private transient YamlGitService yamlGitService;

  @InjectMocks @Inject YamlServiceImpl yamlService = new YamlServiceImpl();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void processYamlFilesAsTar() throws IOException {
    InputStream zipfile = getClass().getResourceAsStream("yaml_zip_test.zip");

    List<GitFileChange> changeList = yamlService.getChangesForZipFile("TestAccountID", zipfile, null);
    assertThat(changeList.size()).isEqualTo(59);
    assertThat(changeList.stream()
                   .filter(change
                       -> change.getFilePath().equals(
                           "Setup_Master_Copy/Applications/Harness-on-prem/Services/MongoDB/Config Files/"))
                   .collect(toList()))
        .isEmpty();
  }

  @Test
  public void testFilterInvalidFilePaths() throws Exception {
    List<GitFileChange> gitFileChange = new ArrayList<>();
    gitFileChange.add(aGitFileChange().withAccountId(ACCOUNT_ID).withFilePath("temp.yaml").build());

    List<GitFileChange> filteredGitFileChange =
        (List<GitFileChange>) MethodUtils.invokeMethod(yamlService, true, "filterInvalidFilePaths", gitFileChange);
    assertNotNull(filteredGitFileChange);
    assertEquals(0, filteredGitFileChange.size());

    String validFilePath = "Setup/Applications/app1/Index.yaml";
    gitFileChange.add(aGitFileChange().withAccountId(ACCOUNT_ID).withFilePath(validFilePath).build());
    assertEquals(2, gitFileChange.size());

    filteredGitFileChange =
        (List<GitFileChange>) MethodUtils.invokeMethod(yamlService, true, "filterInvalidFilePaths", gitFileChange);
    assertNotNull(filteredGitFileChange);
    assertEquals(1, filteredGitFileChange.size());
    assertEquals(validFilePath, filteredGitFileChange.get(0).getFilePath());

    gitFileChange.clear();
    gitFileChange.add(aGitFileChange().withAccountId(ACCOUNT_ID).withFilePath(validFilePath).build());
    filteredGitFileChange =
        (List<GitFileChange>) MethodUtils.invokeMethod(yamlService, true, "filterInvalidFilePaths", gitFileChange);
    assertNotNull(filteredGitFileChange);
    assertEquals(1, filteredGitFileChange.size());
    assertEquals(validFilePath, filteredGitFileChange.get(0).getFilePath());
  }
}
