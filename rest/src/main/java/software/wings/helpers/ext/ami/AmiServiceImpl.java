package software.wings.helpers.ext.ami;

import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import software.wings.beans.AwsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sgurubelli on 12/14/17.
 */
public class AmiServiceImpl implements AmiService {
  @Inject private AwsHelperService awsHelperService;
  @Inject private EncryptionService encryptionService;

  @Override
  public List<BuildDetails> getBuilds(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String tag, int maxNumberOfBuilds) {
    List<BuildDetails> buildDetails = new ArrayList<>();
    DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
    DescribeImagesResult describeImagesResult;
    encryptionService.decrypt(awsConfig, encryptionDetails);
    describeImagesResult =
        awsHelperService.describeImagesResult(awsConfig, encryptionDetails, region, describeImagesRequest);
    describeImagesResult.getImages()
        .stream()
        .filter(imageIdentifier -> imageIdentifier != null && !Misc.isNullOrEmpty(imageIdentifier.getName()))
        .forEach(imageIdentifier
            -> buildDetails.add(BuildDetails.Builder.aBuildDetails().withNumber(imageIdentifier.getName()).build()));
    return buildDetails;
  }

  @Override
  public List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    return awsHelperService.listRegions(awsConfig, encryptionDetails);
  }
}
