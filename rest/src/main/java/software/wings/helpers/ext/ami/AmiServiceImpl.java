package software.wings.helpers.ext.ami;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.utils.Misc.isNullOrEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 12/14/17.
 */
@Singleton
public class AmiServiceImpl implements AmiService {
  private final static Logger logger = LoggerFactory.getLogger(AmiServiceImpl.class);
  @Inject private AwsHelperService awsHelperService;

  @Override
  public List<BuildDetails> getBuilds(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      Map<String, List<String>> tags, String platform, int maxNumberOfBuilds) {
    logger.info("Retrieving images from Aws");
    List<BuildDetails> buildDetails = new ArrayList<>();
    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("is-public").withValues("false"));
    filters.add(new Filter("state").withValues("available"));
    if (!isNullOrEmpty(platform)) {
      filters.add(new Filter("platform").withValues(platform));
    }
    if (tags != null && tags.size() != 0) {
      tags.keySet().forEach(key -> filters.add(new Filter("tag:" + key, new ArrayList<>(tags.get(key)))));
    }
    DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest().withFilters(filters);
    DescribeImagesResult describeImagesResult;
    describeImagesResult =
        awsHelperService.decribeEc2Images(awsConfig, encryptionDetails, region, describeImagesRequest);
    logger.info("Sorting on creation time");
    Collections.sort(describeImagesResult.getImages(), Comparator.comparing(Image::getCreationDate));
    describeImagesResult.getImages()
        .stream()
        .filter(image -> image != null && !isNullOrEmpty(image.getName()))
        .forEach(image
            -> buildDetails.add(aBuildDetails().withNumber(image.getName()).withRevision(image.getImageId()).build()));
    if (buildDetails.size() == 0) {
      logger.info("No images found matching with the given Region {}, and filters {}", region, filters);
    }
    return buildDetails;
  }
}
