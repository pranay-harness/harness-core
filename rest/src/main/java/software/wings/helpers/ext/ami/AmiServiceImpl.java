package software.wings.helpers.ext.ami;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.utils.Misc.isNullOrEmpty;

import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;
import software.wings.beans.AwsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by sgurubelli on 12/14/17.
 */
@Singleton
public class AmiServiceImpl implements AmiService {
  @Inject private AwsHelperService awsHelperService;

  @Override
  public List<BuildDetails> getBuilds(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      Multimap<String, String> tags, int maxNumberOfBuilds) {
    List<BuildDetails> buildDetails = new ArrayList<>();
    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("is-public").withValues("false"));
    filters.add(new Filter("state").withValues("available"));
    if (tags != null) {
      tags.keySet().forEach(key -> filters.add(new Filter("tag:" + key, new ArrayList<>(tags.get(key)))));
    }
    DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest().withFilters(filters);
    DescribeImagesResult describeImagesResult;
    describeImagesResult =
        awsHelperService.decribeEc2Images(awsConfig, encryptionDetails, region, describeImagesRequest);
    Collections.sort(
        describeImagesResult.getImages(), (o1, o2) -> o2.getCreationDate().compareTo(o1.getCreationDate()));
    describeImagesResult.getImages()
        .stream()
        .filter(image -> image != null && !isNullOrEmpty(image.getName()))
        .forEach(image
            -> buildDetails.add(aBuildDetails().withNumber(image.getName()).withRevision(image.getImageId()).build()));
    return buildDetails;
  }

  @Override
  public List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    return awsHelperService.listRegions(awsConfig, encryptionDetails);
  }
}
