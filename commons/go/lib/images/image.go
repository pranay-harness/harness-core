// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package images

import (
	"github.com/google/go-containerregistry/pkg/authn"
	"github.com/google/go-containerregistry/pkg/name"
	"github.com/google/go-containerregistry/pkg/v1/remote"
	"github.com/pkg/errors"
	k8v1 "k8s.io/api/core/v1"
)

// PublicMetadata returns entrypoint & commands for public docker image
func PublicMetadata(image string) ([]string, []string, error) {
	return getImageMetadata(image, remote.WithAuth(authn.Anonymous))
}

// PrivateMetadata returns entrypoint & commands for private image.
// It takes docker config secret in json format for image as an input.
func PrivateMetadata(image, dockerCfg string) ([]string, []string, error) {
	secret := k8v1.Secret{
		Data: map[string][]byte{
			k8v1.DockerConfigKey: []byte(dockerCfg),
		},
		Type: k8v1.SecretTypeDockercfg,
	}
	kc, err := getImageKeyChain([]k8v1.Secret{secret})
	if err != nil {
		return nil, nil, errors.Wrap(err, "error creating k8schain")
	}

	mkc := authn.NewMultiKeychain(kc)
	return getImageMetadata(image, remote.WithAuthFromKeychain(mkc))
}

// CombinedEntrypoint returns combined entrypoint of a docker image
func CombinedEntrypoint(ep, cmds []string) []string {
	return append(ep, cmds...)
}

// Returns entrypoint and commands of a docker image
func getImageMetadata(image string, authOpt remote.Option) ([]string, []string, error) {
	ref, err := name.ParseReference(image, name.WeakValidation)
	if err != nil {
		return nil, nil, err
	}

	img, err := remote.Image(ref, authOpt)
	if err != nil {
		return nil, nil, err
	}

	ep, cmds, _, err := getImageData(ref, img)
	if err != nil {
		return nil, nil, err
	}
	return ep, cmds, nil
}
