package io.harness.generator;

import static io.harness.generator.ServiceGenerator.Services.KUBERNETES_GENERIC_TEST;
import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Service.APP_ID_KEY;
import static software.wings.beans.Service.ServiceBuilder;
import static software.wings.beans.Service.builder;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.artifactstream.ArtifactStreamManager;
import io.harness.generator.artifactstream.ArtifactStreamManager.ArtifactStreams;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.LambdaSpecification.DefaultSpecification;
import software.wings.beans.LambdaSpecification.FunctionSpecification;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@Singleton
public class ServiceGenerator {
  @Inject private OwnerManager ownerManager;
  @Inject ApplicationGenerator applicationGenerator;
  @Inject ArtifactStreamManager artifactStreamManager;

  @Inject ServiceResourceService serviceResourceService;
  @Inject WingsPersistence wingsPersistence;

  public enum Services {
    GENERIC_TEST,
    KUBERNETES_GENERIC_TEST,
    FUNCTIONAL_TEST,
    WINDOWS_TEST,
    ECS_TEST,
    K8S_V2_TEST,
    MULTI_ARTIFACT_FUNCTIONAL_TEST,
    MULTI_ARTIFACT_K8S_V2_TEST
  }

  public Service ensurePredefined(Randomizer.Seed seed, Owners owners, Services predefined) {
    switch (predefined) {
      case GENERIC_TEST:
        return ensureGenericTest(seed, owners, "Test Service");
      case FUNCTIONAL_TEST:
        return ensureFunctionalTest(seed, owners, "FunctionalTest Service");
      case KUBERNETES_GENERIC_TEST:
        return ensureKubernetesGenericTest(seed, owners);
      case WINDOWS_TEST:
        return ensureWindowsTest(seed, owners, "Test IIS APP Service");
      case K8S_V2_TEST:
        return ensureK8sTest(seed, owners, "Test K8sV2 Service");
      case MULTI_ARTIFACT_FUNCTIONAL_TEST:
        return ensureMultiArtifactFunctionalTest(seed, owners, "MA-FunctionalTest Service");
      case MULTI_ARTIFACT_K8S_V2_TEST:
        return ensureMultiArtifactK8sTest(seed, owners, "MA-Test K8sV2 Service");
      default:
        unhandled(predefined);
    }

    return null;
  }

  public Service ensureWindowsTest(Randomizer.Seed seed, Owners owners, String name) {
    owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST));
    owners.add(ensureService(seed, owners, builder().name(name).artifactType(ArtifactType.IIS_APP).build()));
    ArtifactStream artifactStream =
        artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.HARNESS_SAMPLE_IIS_APP);
    Service service = owners.obtainService();
    service.setArtifactStreamIds(new ArrayList<>(Arrays.asList(artifactStream.getUuid())));
    return service;
  }

  public Service ensureK8sTest(Randomizer.Seed seed, Owners owners, String name) {
    owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST));
    owners.add(ensureService(seed, owners,
        builder()
            .name(name)
            .artifactType(ArtifactType.DOCKER)
            .deploymentType(DeploymentType.KUBERNETES)
            .isK8sV2(true)
            .build()));
    ArtifactStream artifactStream =
        artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.HARNESS_SAMPLE_DOCKER);
    Service service = owners.obtainService();
    service.setArtifactStreamIds(new ArrayList<>(Arrays.asList(artifactStream.getUuid())));
    return service;
  }

  public Service ensureMultiArtifactK8sTest(Randomizer.Seed seed, Owners owners, String name) {
    owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST));
    owners.add(ensureService(seed, owners,
        builder()
            .name(name)
            .artifactType(ArtifactType.DOCKER)
            .deploymentType(DeploymentType.KUBERNETES)
            .isK8sV2(true)
            .build()));
    return owners.obtainService();
  }

  public Service ensureEcsTest(Randomizer.Seed seed, Owners owners, String name) {
    owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST));
    owners.add(ensureService(seed, owners, builder().name(name).artifactType(ArtifactType.DOCKER).build()));
    ArtifactStream artifactStream =
        artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.HARNESS_SAMPLE_ECR);
    Service service = owners.obtainService();
    service.setArtifactStreamIds(new ArrayList<>(Arrays.asList(artifactStream.getUuid())));
    return service;
  }

  public Service ensureGenericTest(Randomizer.Seed seed, Owners owners, String name) {
    owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST));
    owners.add(ensureService(
        seed, owners, builder().name(name).artifactType(ArtifactType.WAR).deploymentType(DeploymentType.SSH).build()));
    ArtifactStream artifactStream = artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.AWS_AMI);
    Service service = owners.obtainService();
    service.setArtifactStreamIds(new ArrayList<>(Arrays.asList(artifactStream.getUuid())));
    return service;
  }

  public Service ensureFunctionalTest(Randomizer.Seed seed, Owners owners, String name) {
    owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners, Applications.FUNCTIONAL_TEST));
    owners.add(ensureService(seed, owners, builder().name(name).artifactType(ArtifactType.WAR).build()));
    artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.ARTIFACTORY_ECHO_WAR);
    return owners.obtainService();
  }

  public Service ensureMultiArtifactFunctionalTest(Randomizer.Seed seed, Owners owners, String name) {
    owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners, Applications.FUNCTIONAL_TEST));
    owners.add(ensureService(seed, owners, builder().name(name).artifactType(ArtifactType.WAR).build()));
    return owners.obtainService();
  }

  public Service ensureAmiGenericTest(Randomizer.Seed seed, Owners owners, String name) {
    owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST));
    owners.add(ensureService(seed, owners, builder().name(name).artifactType(ArtifactType.AMI).build()));
    artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.AWS_AMI);
    return owners.obtainService();
  }

  private Service ensureKubernetesGenericTest(Randomizer.Seed seed, Owners owners) {
    owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST));
    return ensureService(
        seed, owners, builder().name(KUBERNETES_GENERIC_TEST.name()).artifactType(ArtifactType.DOCKER).build());
  }

  public Service ensureAwsLambdaGenericTest(Randomizer.Seed seed, Owners owners, String name) {
    owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST));
    owners.add(ensureService(seed, owners, builder().name(name).artifactType(ArtifactType.AWS_LAMBDA).build()));
    ArtifactStream artifactStream =
        artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.HARNESS_EXAMPLE_LAMBDA);
    Service service = owners.obtainService();
    service.setArtifactStreamIds(Collections.singletonList(artifactStream.getUuid()));
    if (serviceResourceService.getLambdaSpecification(service.getAppId(), service.getUuid()) == null) {
      LambdaSpecification lambdaSpecification =
          LambdaSpecification.builder()
              .serviceId(service.getUuid())
              .defaults(DefaultSpecification.builder().runtime("nodejs8.10").memorySize(128).timeout(3).build())
              .functions(Collections.singletonList(FunctionSpecification.builder()
                                                       .functionName("functional-test-lambda")
                                                       .memorySize(128)
                                                       .runtime("nodejs8.10")
                                                       .handler("index.handler")
                                                       .build()))
              .build();
      lambdaSpecification.setAppId(owners.obtainApplication().getUuid());
      serviceResourceService.createLambdaSpecification(lambdaSpecification);
    }
    return service;
  }

  public Service ensureRandom(Randomizer.Seed seed, Owners owners) {
    EnhancedRandom random = Randomizer.instance(seed);
    Services predefined = random.nextObject(Services.class);
    return ensurePredefined(seed, owners, predefined);
  }

  public Service exists(Service service) {
    return wingsPersistence.createQuery(Service.class)
        .filter(APP_ID_KEY, service.getAppId())
        .filter(ServiceKeys.name, service.getName())
        .get();
  }

  public Service ensureService(Randomizer.Seed seed, Owners owners, Service service) {
    EnhancedRandom random = Randomizer.instance(seed);

    ServiceBuilder builder = Service.builder();

    if (service != null && service.getAppId() != null) {
      builder.appId(service.getAppId());
    } else {
      Application application = owners.obtainApplication(() -> applicationGenerator.ensureRandom(seed, owners));
      builder.appId(application.getUuid());
    }

    if (service != null && service.getName() != null) {
      builder.name(service.getName());
    } else {
      builder.name(random.nextObject(String.class));
    }

    Service existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }

    if (service != null && service.getDescription() != null) {
      builder.description(service.getDescription());
    } else {
      builder.description(random.nextObject(String.class));
    }

    if (service != null && service.getArtifactType() != null) {
      builder.artifactType(service.getArtifactType());
    } else {
      builder.artifactType(random.nextObject(ArtifactType.class));
    }

    if (service != null) {
      builder.deploymentType(service.getDeploymentType());
      builder.isK8sV2(service.isK8sV2());
    }

    if (service != null && service.getCreatedBy() != null) {
      builder.createdBy(service.getCreatedBy());
    } else {
      builder.createdBy(owners.obtainUser());
    }

    final Service finalService = builder.build();

    return GeneratorUtils.suppressDuplicateException(
        () -> serviceResourceService.save(finalService), () -> exists(finalService));
  }
}
