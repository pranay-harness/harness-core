// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package images

// This file is taken from https://github.com/google/go-containerregistry/blob/master/pkg/authn/k8schain/k8schain.go#L135

import (
	"fmt"

	"github.com/google/go-containerregistry/pkg/authn"
	"github.com/google/go-containerregistry/pkg/name"
	credentialprovider "github.com/vdemeester/k8s-pkg-credentialprovider"
	credentialprovidersecrets "github.com/vdemeester/k8s-pkg-credentialprovider/secrets"
	k8v1 "k8s.io/api/core/v1"
)

type lazyProvider struct {
	kc    *keychain
	image string
}

// Authorization implements Authenticator.
func (lp lazyProvider) Authorization() (*authn.AuthConfig, error) {
	creds, found := lp.kc.keyring.Lookup(lp.image)
	if !found || len(creds) < 1 {
		return nil, fmt.Errorf("keychain returned no credentials for %q", lp.image)
	}
	authConfig := creds[0]
	return &authn.AuthConfig{
		Username:      authConfig.Username,
		Password:      authConfig.Password,
		Auth:          authConfig.Auth,
		IdentityToken: authConfig.IdentityToken,
		RegistryToken: authConfig.RegistryToken,
	}, nil
}

type keychain struct {
	keyring credentialprovider.DockerKeyring
}

// Resolve implements authn.Keychain
func (kc *keychain) Resolve(target authn.Resource) (authn.Authenticator, error) {
	var image string
	if repo, ok := target.(name.Repository); ok {
		image = repo.String()
	} else {
		// Lookup expects an image reference and we only have a registry.
		image = target.RegistryStr() + "/foo/bar"
	}

	if creds, found := kc.keyring.Lookup(image); !found || len(creds) < 1 {
		return authn.Anonymous, nil
	}
	// TODO(mattmoor): How to support multiple credentials?
	return lazyProvider{
		kc:    kc,
		image: image,
	}, nil
}

// Returns key chain for image secrets
func getImageKeyChain(secrets []k8v1.Secret) (authn.Keychain, error) {
	keyring := credentialprovider.NewDockerKeyring()

	// extend the default keyring with the pull secrets.
	kr, err := credentialprovidersecrets.MakeDockerKeyring(secrets, keyring)
	if err != nil {
		return nil, err
	}
	return &keychain{
		keyring: kr,
	}, nil
}
