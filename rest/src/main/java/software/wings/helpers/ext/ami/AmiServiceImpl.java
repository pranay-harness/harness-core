package software.wings.helpers.ext.ami;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

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
  private static final String AMI_RESOURCE_FILTER_PREFIX = "ami-";
  private static final Logger logger = LoggerFactory.getLogger(AmiServiceImpl.class);
  @Inject private AwsHelperService awsHelperService;

  @Override
  public List<BuildDetails> getBuilds(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      Map<String, List<String>> tags, Map<String, String> filterMap, int maxNumberOfBuilds) {
    logger.info("Retrieving images from Aws");
    List<BuildDetails> buildDetails = new ArrayList<>();
    List<Filter> filters = getFilters(tags, filterMap);
    DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest().withFilters(filters);
    DescribeImagesResult describeImagesResult;
    describeImagesResult =
        awsHelperService.decribeEc2Images(awsConfig, encryptionDetails, region, describeImagesRequest);
    logger.info("Sorting on creation time");
    Collections.sort(describeImagesResult.getImages(), Comparator.comparing(Image::getCreationDate));
    describeImagesResult.getImages()
        .stream()
        .filter(image -> image != null && isNotBlank(image.getName()))
        .forEach(image
            -> buildDetails.add(aBuildDetails().withNumber(image.getName()).withRevision(image.getImageId()).build()));
    if (buildDetails.isEmpty()) {
      logger.info("No images found matching with the given Region {}, and filters {}", region, filters);
    } else {
      logger.info("Images found of size {}", buildDetails.size());
    }
    return buildDetails;
  }

  protected List<Filter> getFilters(Map<String, List<String>> tags, Map<String, String> filterMap) {
    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("is-public").withValues("false"));
    filters.add(new Filter("state").withValues("available"));

    if (isNotEmpty(tags)) {
      tags.keySet().forEach(key -> filters.add(new Filter("tag:" + key, new ArrayList<>(tags.get(key)))));
    }

    if (isNotEmpty(filterMap)) {
      filterMap.entrySet()
          .stream()
          .filter(entry -> isNotBlank(entry.getKey()))
          .filter(entry -> isNotBlank(entry.getValue()))
          .filter(entry -> entry.getKey().startsWith(AMI_RESOURCE_FILTER_PREFIX))
          .forEach(entry -> filters.add(new Filter(entry.getKey().substring(4)).withValues(entry.getValue())));
    }
    return filters;
  }
}
