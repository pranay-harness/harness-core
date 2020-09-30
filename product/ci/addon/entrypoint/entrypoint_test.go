package entrypoint

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestGetPublicImage(t *testing.T) {
	image := "plugins/git"
	cmds, err := GetPublicImage(image)
	fmt.Println(cmds)
	assert.Nil(t, err)
	assert.NotEqual(t, len(cmds), 0)
}

func TestGetPublicImageNameErr(t *testing.T) {
	image := "plugins:g:it"
	_, err := GetPublicImage(image)
	assert.NotNil(t, err)
}
