package software.wings.service.impl.yaml.service;

import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.yaml.YamlGitService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class YamlServiceImplTest {
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
    Assertions.assertThat(changeList.size()).isEqualTo(59);
    Assertions
        .assertThat(changeList.stream()
                        .filter(change
                            -> change.getFilePath().equals(
                                "Setup_Master_Copy/Applications/Harness-on-prem/Services/MongoDB/Config Files/"))
                        .collect(toList()))
        .isEmpty();
  }
}
