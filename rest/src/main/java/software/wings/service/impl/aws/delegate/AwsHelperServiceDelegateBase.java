package software.wings.service.impl.aws.delegate;

import com.google.inject.Inject;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AmazonAutoScalingException;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ecr.model.AmazonECRException;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ClientException;
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.service.intfc.security.EncryptionService;

class AwsHelperServiceDelegateBase {
  private static final Logger logger = LoggerFactory.getLogger(AwsHelperServiceDelegateBase.class);
  @Inject protected EncryptionService encryptionService;

  protected void handleAmazonServiceException(AmazonServiceException amazonServiceException) {
    logger.error("AWS API call exception", amazonServiceException);
    if (amazonServiceException instanceof AmazonCodeDeployException) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED).addParam("message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof AmazonEC2Exception) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED).addParam("message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof ClusterNotFoundException) {
      throw new WingsException(ErrorCode.AWS_CLUSTER_NOT_FOUND)
          .addParam("message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof ServiceNotFoundException) {
      throw new WingsException(ErrorCode.AWS_SERVICE_NOT_FOUND)
          .addParam("message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof AmazonAutoScalingException) {
      logger.warn(amazonServiceException.getErrorMessage(), amazonServiceException);
      throw amazonServiceException;
    } else if (amazonServiceException instanceof AmazonECSException
        || amazonServiceException instanceof AmazonECRException) {
      if (amazonServiceException instanceof ClientException) {
        logger.warn(amazonServiceException.getErrorMessage(), amazonServiceException);
        throw amazonServiceException;
      }
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED).addParam("message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof AmazonCloudFormationException) {
      if (amazonServiceException.getMessage().contains("No updates are to be performed")) {
        logger.info("Nothing to update on stack" + amazonServiceException.getMessage());
      } else {
        throw new InvalidRequestException(amazonServiceException.getMessage(), amazonServiceException);
      }
    } else {
      logger.error("Unhandled aws exception");
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED).addParam("message", amazonServiceException.getMessage());
    }
  }
}