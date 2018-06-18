package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.AwsEc2Service;

import java.util.List;

/**
 * Created by anubhaw on 6/17/18.
 */
@Singleton
public class AwsEc2ServiceImpl implements AwsEc2Service {
  @Inject private AwsHelperService awsHelperService;

  @Override
  public boolean validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    return awsHelperService.validateAwsAccountCredential(awsConfig, encryptionDetails);
  }
}
