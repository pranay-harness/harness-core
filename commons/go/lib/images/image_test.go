// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package images

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestPublicMetadata(t *testing.T) {
	image := "redis"
	ep, cmds, err := PublicMetadata(image)
	fmt.Println(ep)
	fmt.Println(cmds)
	assert.Nil(t, err)
	assert.NotEqual(t, len(cmds), 0)
}

func TestPublicNameErr(t *testing.T) {
	image := "plugins:g:it"
	_, _, err := PublicMetadata(image)
	assert.NotNil(t, err)
}

func TestPrivateImageInvalidDockerCfg(t *testing.T) {
	image := "harness/ci-lite-engine:v0.7-alpha"
	cfg := "hello"
	_, _, err := PrivateMetadata(image, cfg)
	fmt.Println(err)
	assert.NotNil(t, err)
}

func TestPrivateImageKeyChainErr(t *testing.T) {
	image := "harness/ci-lite-engine:v0.7-alpha"
	cfg := "Zm9vLWJhcg=="
	_, _, err := PrivateMetadata(image, cfg)
	fmt.Println(err)
	assert.NotNil(t, err)
}

func TestPrivateImageInvalidSecret(t *testing.T) {
	image := "harness/ci-lite-engine:v0.7-alpha"
	cfg := "eyJodHRwczovL2luZGV4LmRvY2tlci5pby92MS8iOnsidXNlcm5hbWUiOiJmb28iLCJwYXNzd29yZCI6ImJhciJ9fQ=="
	_, _, err := PrivateMetadata(image, cfg)
	fmt.Println(err)
	assert.NotNil(t, err)
}
