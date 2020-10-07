package entrypoint

import (
	"github.com/google/go-containerregistry/pkg/name"
	v1 "github.com/google/go-containerregistry/pkg/v1"
	"github.com/pkg/errors"
)

// getImageData pulls the entrypoint from the image, and returns the given
// original reference, with image digest resolved.
func getImageData(ref name.Reference, img v1.Image) ([]string, name.Digest, error) {
	digest, err := img.Digest()
	if err != nil {
		return nil, name.Digest{}, errors.Wrap(err, "error getting image digest")
	}
	cfg, err := img.ConfigFile()
	if err != nil {
		return nil, name.Digest{}, errors.Wrap(err, "error getting image config")
	}

	// Entrypoint can be specified in either .Config.Entrypoint or
	// .Config.Cmd.
	ep := cfg.Config.Entrypoint
	if len(ep) == 0 {
		ep = cfg.Config.Cmd
	}

	d, err := name.NewDigest(ref.Context().String()+"@"+digest.String(), name.WeakValidation)
	if err != nil {
		return nil, name.Digest{}, errors.Wrap(err, "error constructing resulting digest")
	}
	return ep, d, nil
}
