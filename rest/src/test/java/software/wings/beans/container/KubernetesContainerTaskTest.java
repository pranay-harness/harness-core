package software.wings.beans.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KubernetesContainerTaskTest {
  static final String DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX = "\\$\\{DOCKER_IMAGE_NAME}";
  static final String DOCKER_IMAGE_NAME_REGEX = "(\\s*\"?image\"?\\s*:\\s*\"?)";

  @Test
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
}
