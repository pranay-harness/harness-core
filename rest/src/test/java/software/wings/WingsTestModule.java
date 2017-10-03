package software.wings;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;

import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.helpers.ext.amazons3.AmazonS3ServiceImpl;
import software.wings.service.impl.AmazonS3BuildServiceImpl;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.AmazonS3BuildService;

public class WingsTestModule extends AbstractModule {
  @Override
  protected void configure() {
    DelegateFileManager mockDelegateFileManager = mock(DelegateFileManager.class);
    bind(DelegateFileManager.class).toInstance(mockDelegateFileManager);
    bind(AwsHelperService.class).toInstance(mock(AwsHelperService.class));
    bind(AmazonS3BuildService.class).to(AmazonS3BuildServiceImpl.class);
    bind(AmazonS3Service.class).to(AmazonS3ServiceImpl.class);
  }
}
