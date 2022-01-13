// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

// This code has been adapted from Drone
package logs

import (
	"fmt"
	"testing"
)

func TestReplace(t *testing.T) {
	secrets := []Secret{
		&mockSecret{Name: "DOCKER_USERNAME", Data: "octocat", Mask: false},
		&mockSecret{Name: "DOCKER_PASSWORD", Data: "correct-horse-batter-staple", Mask: true},
		&mockSecret{Name: "DOCKER_EMAIL", Data: "", Mask: true},
	}

	sw := &nopWriter{}
	w := NewReplacer(&nopCloser{sw}, secrets)
	w.Write([]byte("username octocat password correct-horse-batter-staple"))

	if got, want := sw.data[0], fmt.Sprintf("username octocat password %s", maskedString); got != want {
		t.Errorf("Want masked string %s, got %s", want, got)
	}
}

func TestReplaceMultiline(t *testing.T) {
	key := `
-----BEGIN PRIVATE KEY-----
MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEA0SC5BIYpanOv6wSm
dHVVMRa+6iw/0aJpT9/LKcZ0XYQ43P9Vwn8c46MDvFJ+Uy41FwbxT+QpXBoLlp8D
sJY/dQIDAQABAkAesoL2GwtxSNIF2YTli2OZ9RDJJv2nNAPpaZxU4YCrST1AXGPB
tFm0LjYDDlGJ448syKRpdypAyCR2LidwrVRxAiEA+YU5Zv7bOwODCsmtQtIfBfhu
6SMBGMDijK7OYfTtjQsCIQDWjvly6b6doVMdNjqqTsnA8J1ShjSb8bFXkMels941
fwIhAL4Rr7I3PMRtXmrfSa325U7k+Yd59KHofCpyFiAkNLgVAiB8JdR+wnOSQAOY
loVRgC9LXa6aTp9oUGxeD58F6VK9PwIhAIDhSxkrIatXw+dxelt8DY0bEdDbYzky
r9nicR5wDy2W
-----END PRIVATE KEY-----`

	line := `> MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEA0SC5BIYpanOv6wSm`

	secrets := []Secret{
		&mockSecret{Name: "SSH_KEY", Data: key, Mask: true},
	}

	sw := &nopWriter{}
	w := NewReplacer(&nopCloser{sw}, secrets)
	w.Write([]byte(line))
	w.Close()

	if got, want := sw.data[0], fmt.Sprintf("> %s", maskedString); got != want {
		t.Errorf("Want masked string %s, got %s", want, got)
	}
}

func TestReplaceMultilineJson(t *testing.T) {
	key := `{
  "token":"MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEA0SC5BIYpanOv6wSm"
}`

	line := `{
  "token":"MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEA0SC5BIYpanOv6wSm"
}`

	secrets := []Secret{
		&mockSecret{Name: "JSON_KEY", Data: key, Mask: true},
	}

	sw := &nopWriter{}
	w := NewReplacer(&nopCloser{sw}, secrets)
	w.Write([]byte(line))
	w.Close()

	if got, want := sw.data[0], fmt.Sprintf("{\n  %s\n}", maskedString); got != want {
		t.Errorf("Want masked string %s, got %s", want, got)
	}
}

// this test verifies that if there are no secrets to scan and
// mask, the io.WriteCloser is returned as-is.
func TestReplaceNone(t *testing.T) {
	secrets := []Secret{
		&mockSecret{Name: "DOCKER_USERNAME", Data: "octocat", Mask: false},
		&mockSecret{Name: "DOCKER_PASSWORD", Data: "correct-horse-batter-staple", Mask: false},
	}

	sw := &nopWriter{}
	r := NewReplacer(sw, secrets)
	if sw != r {
		t.Errorf("Expect buffer returned with no replacer")
	}
}

type nopCloser struct {
	StreamWriter
}

type mockSecret struct {
	Name string
	Data string
	Mask bool
}

func (s *mockSecret) GetName() string  { return s.Name }
func (s *mockSecret) GetValue() string { return s.Data }
func (s *mockSecret) IsMasked() bool   { return s.Mask }
