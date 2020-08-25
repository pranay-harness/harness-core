package software.wings.service.impl.yaml.service;

import static io.harness.git.model.ChangeType.DELETE;
import static io.harness.git.model.ChangeType.MODIFY;
import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.RUSHABH;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.beans.yaml.Change.Builder.aFileChange;
import static software.wings.beans.yaml.GitFileChange.Builder.aGitFileChange;
import static software.wings.beans.yaml.YamlConstants.APPLICATIONS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.INDEX_YAML;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FILE_FOLDER;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlConstants.SERVICES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;
import static software.wings.utils.Utils.generatePath;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.git.model.ChangeType;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.dl.WingsPersistence;
import software.wings.exception.YamlProcessingException;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.YamlPayload;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class YamlServiceImplTest extends WingsBaseTest {
  @Mock private YamlHandlerFactory yamlHandlerFactory;
  @Mock private YamlHelper yamlHelper;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private transient YamlGitService yamlGitService;
  @Mock private FeatureFlagService featureFlagService;
  @InjectMocks @Inject YamlServiceImpl yamlService = new YamlServiceImpl();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
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
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testFilterInvalidFilePaths() throws Exception {
    List<GitFileChange> gitFileChange = new ArrayList<>();
    gitFileChange.add(aGitFileChange().withAccountId(ACCOUNT_ID).withFilePath("temp.yaml").build());

    List<GitFileChange> filteredGitFileChange = yamlService.filterInvalidFilePaths(gitFileChange);
    assertThat(filteredGitFileChange).isNotNull();
    assertThat(filteredGitFileChange).isEmpty();

    String validFilePath = "Setup/Applications/app1/Index.yaml";
    gitFileChange.add(aGitFileChange().withAccountId(ACCOUNT_ID).withFilePath(validFilePath).build());
    assertThat(gitFileChange).hasSize(2);

    filteredGitFileChange = yamlService.filterInvalidFilePaths(gitFileChange);
    assertThat(filteredGitFileChange).isNotNull();
    assertThat(filteredGitFileChange).hasSize(1);
    assertThat(filteredGitFileChange.get(0).getFilePath()).isEqualTo(validFilePath);

    gitFileChange.clear();
    gitFileChange.add(aGitFileChange().withAccountId(ACCOUNT_ID).withFilePath(validFilePath).build());
    filteredGitFileChange = yamlService.filterInvalidFilePaths(gitFileChange);
    assertThat(filteredGitFileChange).isNotNull();
    assertThat(filteredGitFileChange).hasSize(1);
    assertThat(filteredGitFileChange.get(0).getFilePath()).isEqualTo(validFilePath);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testIsProcessingAllowed() {
    YamlType[] types = YamlType.values();
    Change change = new Change();
    for (YamlType type : types) {
      assertThat(yamlService.isProcessingAllowed(change, type)).isEqualTo(true);
    }
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_computeProcessingOrder() {
    final Change change1 = aFileChange()
                               .withAccountId(ACCOUNT_ID)
                               .withFilePath(generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER,
                                   "app123", SERVICES_FOLDER, "service1", MANIFEST_FOLDER, INDEX_YAML))
                               .withChangeType(MODIFY)
                               .build();
    final Change change2 = aFileChange()
                               .withAccountId(ACCOUNT_ID)
                               .withFilePath(generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER,
                                   "app123", SERVICES_FOLDER, "service1", MANIFEST_FOLDER, INDEX_YAML))
                               .withChangeType(DELETE)
                               .build();
    final Change change3 = aFileChange()
                               .withAccountId(ACCOUNT_ID)
                               .withFilePath(generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER,
                                   "app123", SERVICES_FOLDER, "service1", INDEX_YAML))
                               .withChangeType(MODIFY)
                               .build();
    final Change change4 =
        aFileChange()
            .withAccountId(ACCOUNT_ID)
            .withFilePath(generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER, "app123",
                SERVICES_FOLDER, "service1", MANIFEST_FOLDER, MANIFEST_FILE_FOLDER, "vars.yaml"))
            .withChangeType(DELETE)
            .build();
    final ArrayList<Change> changeList = Lists.newArrayList(change1, change2, change3, change4);
    yamlService.sortByProcessingOrder(changeList);
    assertThat(changeList).isEqualTo(Arrays.asList(change4, change2, change3, change1));
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowInvalidYamlNameExceptionWhenPhaseNameHasDots() {
    YamlPayload yamlPayload = new YamlPayload();
    yamlPayload.setYamlPayload("harnessApiVersion: '1.0'\n"
        + "type: ROLLING\n"
        + "concurrencyStrategy: INFRA\n"
        + "envName: qa\n"
        + "phases:\n"
        + "- type: KUBERNETES\n"
        + "  computeProviderName: Harness Sample K8s Cloud Provider\n"
        + "  daemonSet: false\n"
        + "  infraDefinitionName: K8s\n"
        + "  name: Rolling.\n");

    shouldThrowInvalidYamlNameException(yamlPayload);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowInvalidYamlNameExceptionWhenAmiTagNameIsEmpty() {
    YamlPayload yamlPayload = new YamlPayload();
    yamlPayload.setYamlPayload("harnessApiVersion: '1.0'\n"
        + "type: AMI\n"
        + "amiFilters:\n"
        + "- name: ami-image-id\n"
        + "  value: ss\n"
        + "amiTags:\n"
        + "- name: '         '\n"
        + "  value: '123'\n"
        + "region: us-east-1\n"
        + "serverName: aws-playground\n");

    shouldThrowInvalidYamlNameException(yamlPayload);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowInvalidYamlNameExceptionWhenAmiFilterNameIsEmpty() {
    YamlPayload yamlPayload = new YamlPayload();
    yamlPayload.setYamlPayload("harnessApiVersion: '1.0'\n"
        + "type: AMI\n"
        + "amiFilters:\n"
        + "- name: '   '\n"
        + "  value: ss\n"
        + "amiTags:\n"
        + "- name: 'tagName'\n"
        + "  value: '123'\n"
        + "region: us-east-1\n"
        + "serverName: aws-playground\n");

    shouldThrowInvalidYamlNameException(yamlPayload);
  }

  private void shouldThrowInvalidYamlNameException(YamlPayload yamlPayload) {
    GitFileChange change = GitFileChange.Builder.aGitFileChange()
                               .withChangeType(ChangeType.MODIFY)
                               .withFileContent(yamlPayload.getYaml())
                               .withFilePath(generatePath(PATH_DELIMITER, false, SETUP_FOLDER, APPLICATIONS_FOLDER,
                                   "app123", SERVICES_FOLDER, "service1", MANIFEST_FOLDER, INDEX_YAML))
                               .withAccountId("TestAccountID")
                               .build();

    assertThatThrownBy(() -> yamlService.processChangeSet(Collections.singletonList(change)))
        .isInstanceOf(YamlProcessingException.class)
        .hasMessageEndingWith("Error while processing some yaml files in the changeset.");
  }
}
