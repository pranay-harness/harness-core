use crate::java_class;
use lazy_static::lazy_static;
use multimap::MultiMap;
use regex::Regex;
use std::collections::HashSet;
use std::fs;
use std::hash::{Hash, Hasher};
use std::iter::FromIterator;
use std::process::Command;

#[derive(Debug)]
pub struct JavaClass {
    pub name: String,
    pub location: String,
    pub dependencies: HashSet<String>,
    pub target_module: Option<String>,
}

impl PartialEq for JavaClass {
    fn eq(&self, other: &Self) -> bool {
        self.name == other.name
    }
}

impl Eq for JavaClass {}

impl Hash for JavaClass {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.name.hash(state);
    }
}

lazy_static! {
    static ref GIT_REPO_ROOT_DIR: String = String::from_utf8(
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

pub fn populate_target_module(location: &String) -> Option<String> {
    let re = Regex::new(r"@TargetModule\(Module._([0-9A-Z_]+)\)").unwrap();
    let code = fs::read_to_string(&format!("{}/{}", GIT_REPO_ROOT_DIR.as_str(), location)).expect(&format!(
        "failed to read file {}/{}",
        GIT_REPO_ROOT_DIR.as_str(),
        location
    ));

    let captures = re.captures(&code);

    if captures.is_none() {
        None
    } else {
        Some(format!(
            "//{}:module",
            captures
                .unwrap()
                .get(1)
                .unwrap()
                .as_str()
                .to_string()
                .to_lowercase()
                .replace('_', "-")
        ))
    }
}

pub fn class_dependencies(name: &str, dependencies: &MultiMap<String, String>) -> HashSet<String> {
    let deps = dependencies.get_vec(name);
    if deps.is_none() {
        HashSet::new()
    } else {
        HashSet::from_iter(dependencies.get_vec(name).unwrap().iter().cloned())
    }
}

pub fn external_class(key: &str, dependencies: &MultiMap<String, String>) -> JavaClass {
    JavaClass {
        name: key.to_string(),
        location: "n/a".to_string(),
        dependencies: java_class::class_dependencies(key, &dependencies),
        target_module: None,
    }
}
