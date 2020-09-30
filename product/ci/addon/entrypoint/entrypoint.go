package entrypoint

import (
	"fmt"

	"github.com/google/go-containerregistry/pkg/authn"
	"github.com/google/go-containerregistry/pkg/name"
	v1 "github.com/google/go-containerregistry/pkg/v1"
	"github.com/google/go-containerregistry/pkg/v1/remote"
)

// GetPublicImage returns entrypoint for public docker image
func GetPublicImage(image string) ([]string, error) {
	ref, err := name.ParseReference(image, name.WeakValidation)
	if err != nil {
		return nil, err
	}

	img, err := remote.Image(ref, remote.WithAuth(authn.Anonymous))
	if err != nil {
		return nil, err
	}

	cmds, _, err := getImageData(ref, img)
	if err != nil {
		return nil, err
	}
	return cmds, nil
}

// getImageData pulls the entrypoint from the image, and returns the given
// original reference, with image digest resolved.
func getImageData(ref name.Reference, img v1.Image) ([]string, name.Digest, error) {
	digest, err := img.Digest()
	if err != nil {
		return nil, name.Digest{}, fmt.Errorf("error getting image digest: %v", err)
	}
	cfg, err := img.ConfigFile()
	if err != nil {
		return nil, name.Digest{}, fmt.Errorf("error getting image config: %v", err)
	}

	// Entrypoint can be specified in either .Config.Entrypoint or
	// .Config.Cmd.
	ep := cfg.Config.Entrypoint
	if len(ep) == 0 {
		ep = cfg.Config.Cmd
	}

	d, err := name.NewDigest(ref.Context().String()+"@"+digest.String(), name.WeakValidation)
	if err != nil {
		return nil, name.Digest{}, fmt.Errorf("error constructing resulting digest: %v", err)
	}
	return ep, d, nil
}
