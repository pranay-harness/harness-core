use lazy_static::lazy_static;
use multimap::MultiMap;
use regex::Regex;
use std::collections::HashSet;
use std::fs;
use std::hash::{Hash, Hasher};
use std::iter::FromIterator;

use crate::repo::GIT_REPO_ROOT_DIR;

#[derive(Debug)]
pub struct JavaClass {
    pub name: String,
    pub location: String,
    pub dependencies: HashSet<String>,
    pub target_module: Option<String>,
    pub break_dependencies_on: HashSet<String>,
}

pub trait JavaClassTraits {
    fn relative_location(&self) -> String;
}

impl JavaClassTraits for &JavaClass {
    fn relative_location(&self) -> String {
        self.location
            .chars()
            .skip(self.location.find('/').unwrap() + 1)
            .collect()
    }
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
    pub static ref TARGET_MODULE_PATTERN: Regex = Regex::new(r"@TargetModule\((Module.)?_([0-9A-Z_]+)\)").unwrap();
    pub static ref BREAK_DEPENDENCY_ON_PATTERN: Regex = Regex::new(r#"@BreakDependencyOn\("([^"]+)"\)"#).unwrap();
}

pub fn populate_internal_info(location: &str, module_type: &str) -> (Option<String>, HashSet<String>) {
    let code = fs::read_to_string(&format!("{}/{}", GIT_REPO_ROOT_DIR.as_str(), location)).expect(&format!(
        "failed to read file {}/{}",
        GIT_REPO_ROOT_DIR.as_str(),
        location
    ));

    let captures_target_module = TARGET_MODULE_PATTERN.captures(&code);
    let target_module = if captures_target_module.is_none() {
        None
    } else {
        Some(format!(
            "//{}:{}",
            captures_target_module
                .unwrap()
                .get(2)
                .unwrap()
                .as_str()
                .to_string()
                .to_lowercase()
                .replace('_', "-"),
            module_type
        ))
    };

    let captures_break_dependency_on = BREAK_DEPENDENCY_ON_PATTERN.captures_iter(&code);
    let break_dependencies_on = captures_break_dependency_on
        .map(|capture| capture.get(1))
        .map(|break_dependency_on| break_dependency_on.unwrap().as_str().to_string())
        .collect::<HashSet<String>>();

    (target_module, break_dependencies_on)
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
        dependencies: class_dependencies(key, &dependencies),
        target_module: Default::default(),
        break_dependencies_on: Default::default(),
    }
}
