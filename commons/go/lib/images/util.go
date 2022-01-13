// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package images

import (
	"github.com/google/go-containerregistry/pkg/name"
	v1 "github.com/google/go-containerregistry/pkg/v1"
	"github.com/pkg/errors"
)

// getImageData pulls the entrypoint from the image, and returns the given
// original reference, with image digest resolved.
func getImageData(ref name.Reference, img v1.Image) ([]string, []string, name.Digest, error) {
	digest, err := img.Digest()
	if err != nil {
		return nil, nil, name.Digest{}, errors.Wrap(err, "error getting image digest")
	}
	cfg, err := img.ConfigFile()
	if err != nil {
		return nil, nil, name.Digest{}, errors.Wrap(err, "error getting image config")
	}

	d, err := name.NewDigest(ref.Context().String()+"@"+digest.String(), name.WeakValidation)
	if err != nil {
		return nil, nil, name.Digest{}, errors.Wrap(err, "error constructing resulting digest")
	}
	return cfg.Config.Entrypoint, cfg.Config.Cmd, d, nil
}
