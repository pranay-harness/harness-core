package software.wings.delegatetasks.citasks.cik8handler.container;

import static java.lang.String.format;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.ContainerResourceParams;
import software.wings.beans.container.ImageDetails;
import software.wings.delegatetasks.citasks.cik8handler.params.CIConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerSpecBuilderTestHelper {
  private static String containerName = "container";
  private static List<String> commands = Arrays.asList("/bin/sh", "-c");
  private static List<String> args = Arrays.asList("echo hello_world");

  private static String var1 = "hello";
  private static String value1 = "world";
  private static String var2 = "foo";
  private static String value2 = "bar";

  private static String volume1 = "volume1";
  private static String mountPath1 = "/mnt1";
  private static String volume2 = "volume2";
  private static String mountPath2 = "/mnt2";

  private static String imageName = "IMAGE";
  private static String tag = "TAG";
  private static String imageCtrName = imageName + ":" + tag;
  private static String namespace = "default";
  private static String registryUrl = "https://index.docker.io/v1/";
  private static String userName = "usr";
  private static String password = "pwd";
  private static String registrySecretName = "hs-index-docker-io-v1-usr-hs";

  private static Integer requestMemoryMib = 512;
  private static Integer limitMemoryMib = 1024;
  private static Integer requestMilliCpu = 100;
  private static Integer limitMilliCpu = 200;

  public static CIK8ContainerParams basicCreateSpecInput() {
    ImageDetails imageDetailsWithoutCred =
        ImageDetails.builder().name(imageName).tag(tag).registryUrl(registryUrl).build();
    return CIK8ContainerParams.builder()
        .name(containerName)
        .imageDetails(imageDetailsWithoutCred)
        .commands(commands)
        .args(args)
        .build();
  }

  public static ContainerSpecBuilderResponse basicCreateSpecResponse() {
    ContainerBuilder builder =
        new ContainerBuilder().withName(containerName).withArgs(args).withCommand(commands).withImage(imageCtrName);
    return ContainerSpecBuilderResponse.builder().containerBuilder(builder).build();
  }

  public static CIK8ContainerParams basicCreateSpecWithEnvInput() {
    ImageDetails imageWithoutCred = ImageDetails.builder().name(imageName).tag(tag).registryUrl(registryUrl).build();
    Map<String, String> envVars = new HashMap<>();
    envVars.put(var1, value1);
    envVars.put(var2, value2);

    return CIK8ContainerParams.builder()
        .name(containerName)
        .imageDetails(imageWithoutCred)
        .commands(commands)
        .args(args)
        .envVars(envVars)
        .build();
  }

  public static ContainerSpecBuilderResponse basicCreateSpecWithEnvResponse() {
    Map<String, String> envVars = new HashMap<>();
    List<EnvVar> ctrEnvVars = new ArrayList<>();
    envVars.put(var1, value1);
    envVars.put(var2, value2);
    envVars.forEach((name, val) -> ctrEnvVars.add(new EnvVarBuilder().withName(name).withValue(val).build()));

    ContainerBuilder builder = new ContainerBuilder()
                                   .withName(containerName)
                                   .withArgs(args)
                                   .withCommand(commands)
                                   .withImage(imageCtrName)
                                   .withEnv(ctrEnvVars);
    return ContainerSpecBuilderResponse.builder().containerBuilder(builder).build();
  }

  public static CIK8ContainerParams createSpecWithVolumeMountInput() {
    Map<String, String> envVars = new HashMap<>();
    envVars.put(var1, value1);
    envVars.put(var2, value2);
    ImageDetails imageWithoutCred = ImageDetails.builder().name(imageName).tag(tag).registryUrl(registryUrl).build();

    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(volume1, mountPath1);
    volumeToMountPath.put(volume2, mountPath2);

    return CIK8ContainerParams.builder()
        .name(containerName)
        .imageDetails(imageWithoutCred)
        .commands(commands)
        .args(args)
        .envVars(envVars)
        .volumeToMountPath(volumeToMountPath)
        .build();
  }

  public static ContainerSpecBuilderResponse createSpecWithVolumeMountResponse() {
    Map<String, String> envVars = new HashMap<>();
    List<EnvVar> ctrEnvVars = new ArrayList<>();
    envVars.put(var1, value1);
    envVars.put(var2, value2);
    envVars.forEach((name, val) -> ctrEnvVars.add(new EnvVarBuilder().withName(name).withValue(val).build()));

    List<VolumeMount> ctrVolumeMounts = new ArrayList<>();
    ctrVolumeMounts.add(new VolumeMountBuilder().withName(volume1).withMountPath(mountPath1).build());
    ctrVolumeMounts.add(new VolumeMountBuilder().withName(volume2).withMountPath(mountPath2).build());

    ContainerBuilder builder = new ContainerBuilder()
                                   .withName(containerName)
                                   .withArgs(args)
                                   .withCommand(commands)
                                   .withImage(imageCtrName)
                                   .withEnv(ctrEnvVars)
                                   .withVolumeMounts(ctrVolumeMounts);
    return ContainerSpecBuilderResponse.builder().containerBuilder(builder).build();
  }

  public static CIK8ContainerParams createSpecWithImageCredInput() {
    Map<String, String> envVars = new HashMap<>();
    envVars.put(var1, value1);
    envVars.put(var2, value2);
    ImageDetails imageDetailsWithCred = ImageDetails.builder()
                                            .name(imageName)
                                            .tag(tag)
                                            .registryUrl(registryUrl)
                                            .username(userName)
                                            .password(password)
                                            .build();
    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(volume1, mountPath1);
    volumeToMountPath.put(volume2, mountPath2);

    return CIK8ContainerParams.builder()
        .name(containerName)
        .imageDetails(imageDetailsWithCred)
        .commands(commands)
        .args(args)
        .envVars(envVars)
        .volumeToMountPath(volumeToMountPath)
        .build();
  }

  public static ContainerSpecBuilderResponse createSpecWithImageCredResponse() {
    Map<String, String> envVars = new HashMap<>();
    List<EnvVar> ctrEnvVars = new ArrayList<>();
    envVars.put(var1, value1);
    envVars.put(var2, value2);
    envVars.forEach((name, val) -> ctrEnvVars.add(new EnvVarBuilder().withName(name).withValue(val).build()));

    List<VolumeMount> ctrVolumeMounts = new ArrayList<>();
    ctrVolumeMounts.add(new VolumeMountBuilder().withName(volume1).withMountPath(mountPath1).build());
    ctrVolumeMounts.add(new VolumeMountBuilder().withName(volume2).withMountPath(mountPath2).build());

    ContainerBuilder builder = new ContainerBuilder()
                                   .withName(containerName)
                                   .withArgs(args)
                                   .withCommand(commands)
                                   .withImage(imageCtrName)
                                   .withEnv(ctrEnvVars)
                                   .withVolumeMounts(ctrVolumeMounts);
    return ContainerSpecBuilderResponse.builder()
        .containerBuilder(builder)
        .imageSecret(new LocalObjectReference(registrySecretName))
        .build();
  }

  public static CIK8ContainerParams createSpecWithResourcesCredInput() {
    Map<String, String> envVars = new HashMap<>();
    envVars.put(var1, value1);
    envVars.put(var2, value2);
    ImageDetails imageDetailsWithCred = ImageDetails.builder()
                                            .name(imageName)
                                            .tag(tag)
                                            .registryUrl(registryUrl)
                                            .username(userName)
                                            .password(password)
                                            .build();
    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(volume1, mountPath1);
    volumeToMountPath.put(volume2, mountPath2);

    ContainerResourceParams resourceParams = ContainerResourceParams.builder()
                                                 .resourceLimitMemoryMiB(limitMemoryMib)
                                                 .resourceRequestMemoryMiB(requestMemoryMib)
                                                 .resourceLimitMilliCpu(limitMilliCpu)
                                                 .resourceRequestMilliCpu(requestMilliCpu)
                                                 .build();

    return CIK8ContainerParams.builder()
        .name(containerName)
        .imageDetails(imageDetailsWithCred)
        .commands(commands)
        .args(args)
        .envVars(envVars)
        .volumeToMountPath(volumeToMountPath)
        .containerResourceParams(resourceParams)
        .build();
  }

  public static ContainerSpecBuilderResponse createSpecWithResourcesResponse() {
    Map<String, String> envVars = new HashMap<>();
    List<EnvVar> ctrEnvVars = new ArrayList<>();
    envVars.put(var1, value1);
    envVars.put(var2, value2);
    envVars.forEach((name, val) -> ctrEnvVars.add(new EnvVarBuilder().withName(name).withValue(val).build()));

    List<VolumeMount> ctrVolumeMounts = new ArrayList<>();
    ctrVolumeMounts.add(new VolumeMountBuilder().withName(volume1).withMountPath(mountPath1).build());
    ctrVolumeMounts.add(new VolumeMountBuilder().withName(volume2).withMountPath(mountPath2).build());

    Quantity ctrRequestMemoryMib = new Quantity(format("%d%s", requestMemoryMib, CIConstants.MEMORY_FORMAT));
    Quantity ctrLimitMemoryMib = new Quantity(format("%d%s", limitMemoryMib, CIConstants.MEMORY_FORMAT));
    Quantity ctrRequestMilliCpu = new Quantity(format("%d%s", requestMilliCpu, CIConstants.CPU_FORMAT));
    Quantity ctrLimitMilliCpu = new Quantity(format("%d%s", limitMilliCpu, CIConstants.CPU_FORMAT));
    ResourceRequirements resourceRequirements = new ResourceRequirementsBuilder()
                                                    .addToLimits(CIConstants.MEMORY, ctrLimitMemoryMib)
                                                    .addToRequests(CIConstants.MEMORY, ctrRequestMemoryMib)
                                                    .addToRequests(CIConstants.CPU, ctrRequestMilliCpu)
                                                    .addToLimits(CIConstants.CPU, ctrLimitMilliCpu)
                                                    .build();

    ContainerBuilder builder = new ContainerBuilder()
                                   .withName(containerName)
                                   .withArgs(args)
                                   .withCommand(commands)
                                   .withImage(imageCtrName)
                                   .withEnv(ctrEnvVars)
                                   .withVolumeMounts(ctrVolumeMounts)
                                   .withResources(resourceRequirements);
    return ContainerSpecBuilderResponse.builder()
        .containerBuilder(builder)
        .imageSecret(new LocalObjectReference(registrySecretName))
        .build();
  }
}
