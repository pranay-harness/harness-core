package software.wings.beans.container;

import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BRETT;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KubernetesContainerTaskTest extends WingsBaseTest {
  static final String DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX = "\\$\\{DOCKER_IMAGE_NAME}";
  static final String DOCKER_IMAGE_NAME_REGEX = "(\\s*\"?image\"?\\s*:\\s*\"?)";

  @Mock private AppService appService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject @InjectMocks private ServiceResourceService serviceResourceService;

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldCheckDaemonSet() {
    KubernetesContainerTask kubernetesContainerTask = new KubernetesContainerTask();
    kubernetesContainerTask.setAdvancedConfig("a: b\n kind: DaemonSet\nfoo: bar");
    assertThat(kubernetesContainerTask.checkDaemonSet()).isTrue();
    kubernetesContainerTask.setAdvancedConfig("a: b\n kind: \"DaemonSet\"\nfoo: bar");
    assertThat(kubernetesContainerTask.checkDaemonSet()).isTrue();
    kubernetesContainerTask.setAdvancedConfig("a: b\n kind:DaemonSet\nfoo: bar");
    assertThat(kubernetesContainerTask.checkDaemonSet()).isTrue();
    kubernetesContainerTask.setAdvancedConfig("a: b\n kind:  DaemonSet\nfoo: bar");
    assertThat(kubernetesContainerTask.checkDaemonSet()).isTrue();
    kubernetesContainerTask.setAdvancedConfig("a: b\n kind:\t\"DaemonSet\"\nfoo: bar");
    assertThat(kubernetesContainerTask.checkDaemonSet()).isTrue();
    kubernetesContainerTask.setAdvancedConfig("a: b\n kind: Deployment\nfoo: bar");
    assertThat(kubernetesContainerTask.checkDaemonSet()).isFalse();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldSetAdvancedConfig() {
    KubernetesContainerTask kubernetesContainerTask = new KubernetesContainerTask();

    kubernetesContainerTask.setAdvancedConfig(null);
    assertThat(kubernetesContainerTask.getAdvancedConfig()).isNull();

    kubernetesContainerTask.setAdvancedConfig("one line");
    assertThat(kubernetesContainerTask.getAdvancedConfig()).isEqualTo("one line");

    kubernetesContainerTask.setAdvancedConfig("a\nb");
    assertThat(kubernetesContainerTask.getAdvancedConfig()).isEqualTo("a\nb");

    kubernetesContainerTask.setAdvancedConfig("a \nb");
    assertThat(kubernetesContainerTask.getAdvancedConfig()).isEqualTo("a\nb");

    kubernetesContainerTask.setAdvancedConfig("a    \n b   \n  c");
    assertThat(kubernetesContainerTask.getAdvancedConfig()).isEqualTo("a\n b\n  c");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void validateDomainNameReplacement() {
    String domainName = "abc.xyz.com";
    Pattern pattern = ContainerTask.compileRegexPattern(domainName);

    String imageNameText = "   image: abc.xyz.com/${DOCKER_IMAGE_NAME}";
    Matcher matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(true);

    imageNameText = "   image: \"abc.xyz.com/${DOCKER_IMAGE_NAME}\"";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(true);

    imageNameText = "   \"image\": \"abc.xyz.com/${DOCKER_IMAGE_NAME}\"";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(true);

    imageNameText = "   \"image\": abc.xyz.com/${DOCKER_IMAGE_NAME}";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(true);

    imageNameText = "   \"image\": abcdxyz.com/${DOCKER_IMAGE_NAME}";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(false);

    imageNameText = "   \"image\": pqr*/${DOCKER_IMAGE_NAME}";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(false);

    imageNameText = "   \"image\": abc.xyz.com/(+/)/${DOCKER_IMAGE_NAME}";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(false);

    imageNameText = "   image: ${DOCKER_IMAGE_NAME}";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(false);

    imageNameText = "   image: \"${DOCKER_IMAGE_NAME}\"";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(false);

    imageNameText = "   \"image\": \"${DOCKER_IMAGE_NAME}\"";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(false);

    imageNameText = "   \"image\": ${DOCKER_IMAGE_NAME}";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(false);

    domainName = "abc*";
    pattern = ContainerTask.compileRegexPattern(domainName);

    imageNameText = "   image: abc*/${DOCKER_IMAGE_NAME}";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(true);

    imageNameText = "   image: abc/${DOCKER_IMAGE_NAME}";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(false);

    imageNameText = "   image: \"abc.xyz.com/${DOCKER_IMAGE_NAME}\"";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(false);

    imageNameText = "   \"image\": \"abc.xyz.com/${DOCKER_IMAGE_NAME}\"";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(false);

    imageNameText = "   image: abc.xyz.com/${DOCKER_IMAGE_NAME}";
    matcher = pattern.matcher(imageNameText);
    assertThat(matcher.find()).isEqualTo(false);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDecimalCPUInKubernetesContainerTask() {
    wingsPersistence.save(Service.builder().uuid(SERVICE_ID).appId(APP_ID).build());
    wingsPersistence.save(anApplication().uuid(APP_ID).build());

    KubernetesContainerTask kubernetesContainerTask = new KubernetesContainerTask();
    ContainerDefinition containerDefinition = ContainerDefinition.builder().memory(256).cpu(0.5).build();
    kubernetesContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
    kubernetesContainerTask.setServiceId(SERVICE_ID);
    kubernetesContainerTask.setAppId(APP_ID);

    ContainerTask containerTask = serviceResourceService.createContainerTask(kubernetesContainerTask, false);
    assertThat(containerTask.getContainerDefinitions().get(0).getCpu()).isEqualTo(0.5);
  }
}
