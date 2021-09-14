// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package awsutils

import (
	"crypto/tls"
	"net/http"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/credentials"
	"github.com/aws/aws-sdk-go/aws/session"
)

// NewS3Session creates a new S3 session and returns it.
// Set enableTLSV2 boolean to True to enable TLSV2.
func NewS3Session(accessKey, secretKey, token, region string, enableTLSV2 bool) (*session.Session, error) {
	cfg := &aws.Config{
		Region:      aws.String(region),
		Credentials: credentials.NewStaticCredentials(accessKey, secretKey, token),
	}
	if enableTLSV2 {
		tr := &http.Transport{
			TLSClientConfig: &tls.Config{
				MinVersion: tls.VersionTLS12,
			},
			ForceAttemptHTTP2: true,
		}
		cfg.HTTPClient = &http.Client{Transport: tr}
	}
	return session.NewSession(cfg)
}
