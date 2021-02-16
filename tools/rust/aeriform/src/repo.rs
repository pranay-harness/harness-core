use lazy_static::lazy_static;
use std::process::Command;

lazy_static! {
    pub static ref GIT_REPO_ROOT_DIR: String = String::from_utf8(
        Command::new("git")
            .args(&["rev-parse", "--show-toplevel"])
            .output()
            .unwrap()
            .stdout
    )
    .unwrap()
    .trim()
    .to_string();
}
