package entrypoint

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestPublicImage(t *testing.T) {
	image := "plugins/git"
	cmds, err := PublicImage(image)
	fmt.Println(cmds)
	assert.Nil(t, err)
	assert.NotEqual(t, len(cmds), 0)
}

func TestPublicImageNameErr(t *testing.T) {
	image := "plugins:g:it"
	_, err := PublicImage(image)
	assert.NotNil(t, err)
}

func TestPrivateImageInvalidDockerCfg(t *testing.T) {
	image := "harness/ci-lite-engine:v0.7-alpha"
	cfg := "hello"
	_, err := PrivateImage(image, cfg)
	fmt.Println(err)
	assert.NotNil(t, err)
}

func TestPrivateImageKeyChainErr(t *testing.T) {
	image := "harness/ci-lite-engine:v0.7-alpha"
	cfg := "Zm9vLWJhcg=="
	_, err := PrivateImage(image, cfg)
	fmt.Println(err)
	assert.NotNil(t, err)
}

func TestPrivateImageInvalidSecret(t *testing.T) {
	image := "harness/ci-lite-engine:v0.7-alpha"
	cfg := "eyJodHRwczovL2luZGV4LmRvY2tlci5pby92MS8iOnsidXNlcm5hbWUiOiJmb28iLCJwYXNzd29yZCI6ImJhciJ9fQ=="
	_, err := PrivateImage(image, cfg)
	fmt.Println(err)
	assert.NotNil(t, err)
}
