package software.wings;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.helpers.ext.amazons3.AmazonS3ServiceImpl;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.bamboo.BambooServiceImpl;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.helpers.ext.jenkins.JenkinsImpl;
import software.wings.service.impl.AmazonS3BuildServiceImpl;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.BambooBuildServiceImpl;
import software.wings.service.impl.JenkinsBuildServiceImpl;
import software.wings.service.intfc.AmazonS3BuildService;
import software.wings.service.intfc.BambooBuildService;
import software.wings.service.intfc.JenkinsBuildService;

public class WingsTestModule extends AbstractModule {
  @Override
  protected void configure() {
    DelegateFileManager mockDelegateFileManager = mock(DelegateFileManager.class);
    bind(DelegateFileManager.class).toInstance(mockDelegateFileManager);
    bind(AwsHelperService.class).toInstance(mock(AwsHelperService.class));
    bind(AmazonS3BuildService.class).to(AmazonS3BuildServiceImpl.class);
    bind(AmazonS3Service.class).to(AmazonS3ServiceImpl.class);
    bind(BambooService.class).to(BambooServiceImpl.class);
    bind(BambooBuildService.class).to(BambooBuildServiceImpl.class);
    bind(JenkinsBuildService.class).to(JenkinsBuildServiceImpl.class);

    install(new FactoryModuleBuilder().implement(Jenkins.class, JenkinsImpl.class).build(JenkinsFactory.class));
  }
}
