use regex::Regex;
use std::collections::HashSet;
use std::fs;
use std::hash::{Hash, Hasher};

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

pub fn populate_target_module(location: &String) -> Option<String> {
    let re = Regex::new(r"@TargetModule\(Module._([0-9A-Z_]+)\)").unwrap();
    let code = fs::read_to_string(&format!("/Users/george/github/portal/{}", location))
        .expect(&format!("failed to read file {}", location));

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
