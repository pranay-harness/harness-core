// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package artifacts

import (
	"fmt"

	"github.com/pkg/errors"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
)

//Validates the publish artifact request
func validatePublishRequest(files []*pb.UploadFile, images []*pb.BuildPublishImage) error {
	for _, file := range files {
		err := validateFile(file)
		if err != nil {
			return err
		}
	}

	for _, image := range images {
		err := validateImage(image)
		if err != nil {
			return err
		}
	}
	return nil
}

// Validate a single file.
func validateFile(file *pb.UploadFile) error {
	if file.GetFilePattern() == "" {
		return errors.New("file pattern is not set")
	}
	destination := file.GetDestination()
	authType := destination.GetConnector().GetAuth()
	// Validate auth type for the specified file
	if destination.GetLocationType() == pb.LocationType_JFROG {
		if authType != pb.AuthType_BASIC_AUTH {
			return errors.New(fmt.Sprintf("Unsupported authorization method for JFROG: %s", authType.String()))
		}
	}
	if destination.GetLocationType() == pb.LocationType_S3 {
		if authType != pb.AuthType_ACCESS_KEY {
			return errors.New(fmt.Sprintf("Unsupported authorization method for S3: %s", authType.String()))
		}
		if destination.GetRegion() == "" {
			return errors.New("s3 region is not set")
		}
	}
	if err := validateDestination(destination); err != nil {
		return err
	}
	return nil
}

// Validate a single image. Path to the dockerfile and the context should be set.
func validateImage(image *pb.BuildPublishImage) error {
	if image.GetDockerFile() == "" {
		return errors.New("Docker file path is not set")
	}
	if image.GetContext() == "" {
		return errors.New("Docker file context is not set")
	}
	destination := image.GetDestination()
	authType := destination.GetConnector().GetAuth()
	// Validate auth type for the specified registry
	if destination.GetLocationType() == pb.LocationType_GCR {
		if authType != pb.AuthType_SECRET_FILE {
			return errors.New(fmt.Sprintf("Unsupported authorization method for GCR: %s", authType.String()))
		}
	} else if destination.GetLocationType() == pb.LocationType_ECR {
		if authType != pb.AuthType_ACCESS_KEY {
			return errors.New(fmt.Sprintf("Unsupported authorization method for ECR: %s", authType.String()))
		}
	} else if destination.GetLocationType() == pb.LocationType_DOCKERHUB {
		if authType != pb.AuthType_BASIC_AUTH {
			return errors.New(fmt.Sprintf("Unsupported authorization method for DockerHub: %s", authType.String()))
		}
	}
	if err := validateDestination(destination); err != nil {
		return err
	}
	return nil
}

// Validate the destination. The destination URL should be set and the connector
// should be valid.
func validateDestination(in *pb.Destination) error {
	if in.GetDestinationUrl() == "" {
		return fmt.Errorf("artifact destination url is not set")
	}
	if in.GetConnector().GetId() == "" {
		return fmt.Errorf("connector ID is not set")
	}
	if in.GetLocationType() == pb.LocationType_UNKNOWN {
		return errors.New(fmt.Sprintf("Unsupported location type"))
	}
	return nil
}
