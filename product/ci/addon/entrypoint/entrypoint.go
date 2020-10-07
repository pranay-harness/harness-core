package entrypoint

import (
	"encoding/base64"

	"github.com/google/go-containerregistry/pkg/authn"
	"github.com/google/go-containerregistry/pkg/name"
	"github.com/google/go-containerregistry/pkg/v1/remote"
	"github.com/pkg/errors"
	k8v1 "k8s.io/api/core/v1"
)

// PublicImage returns entrypoint for public docker image
func PublicImage(image string) ([]string, error) {
	return getImageEntrypoint(image, remote.WithAuth(authn.Anonymous))
}

// PrivateImage returns entrypoint for private image.
// It takes base64 encoded docker config secret for image as an input.
func PrivateImage(image, dockerCfg string) ([]string, error) {
	decoded, err := base64.StdEncoding.DecodeString(dockerCfg)
	if err != nil {
		return nil, errors.Wrap(err, "failed to decode docker config secret")
	}

	secret := k8v1.Secret{
		Data: map[string][]byte{
			k8v1.DockerConfigKey: decoded,
		},
		Type: k8v1.SecretTypeDockercfg,
	}
	kc, err := getImageKeyChain([]k8v1.Secret{secret})
	if err != nil {
		return nil, errors.Wrap(err, "error creating k8schain")
	}

	mkc := authn.NewMultiKeychain(kc)
	return getImageEntrypoint(image, remote.WithAuthFromKeychain(mkc))
}

// Returns entrypoint of an image
func getImageEntrypoint(image string, authOpt remote.Option) ([]string, error) {
	ref, err := name.ParseReference(image, name.WeakValidation)
	if err != nil {
		return nil, err
	}

	img, err := remote.Image(ref, authOpt)
	if err != nil {
		return nil, err
	}

	cmds, _, err := getImageData(ref, img)
	if err != nil {
		return nil, err
	}
	return cmds, nil
}
