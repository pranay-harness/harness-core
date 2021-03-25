use lazy_static::lazy_static;
use multimap::MultiMap;
use rayon::prelude::*;
use regex::Regex;
use std::collections::{HashMap, HashSet};
use std::fs;
use std::path::Path;
use std::process::Command;

use crate::java_class::{class_dependencies, external_class, JavaClass, populate_internal_info};

#[derive(Debug)]
pub struct JavaModule {
    pub name: String,
    pub deprecated: bool,
    pub index: f32,
    pub directory: String,
    pub jar: String,
    pub srcs: HashMap<String, JavaClass>,
    pub dependencies: HashSet<String>,
}

pub fn model_names() -> HashMap<String, String> {
    let mut command = Command::new("bazel");
    command.args(&["query", "//...", "--output", "label_kind"]);
    let output = command.output().expect("not able to run bazel");

    if !output.status.success() {
        panic!(
            "Command executed with failing error code \n   {:?}\n   {:?}",
            command,
            String::from_utf8(output.stderr)
        );
    }

    let suffixes = [":module", ":tests", ":supporter-test", ":abstract-module"];

    let targets = String::from_utf8(output.stdout)
        .unwrap()
        .lines()
        .par_bridge()
        .map(|line| line.to_string())
        .filter(|target| target.starts_with("java_library rule ") || target.starts_with("java_binary rule "))
        .filter(|name| suffixes.iter().any(|suffix| name.ends_with(suffix)))
        .collect::<HashSet<String>>();

    targets
        .iter()
        .map(|target| {
            (
                target.split_whitespace().last().unwrap().to_string(),
                target.split_whitespace().nth(0).unwrap().to_string(),
            )
        })
        .collect::<HashMap<String, String>>()
}

pub fn modules() -> HashMap<String, JavaModule> {
    let module_rules = model_names();

    let modules = module_rules.iter().map(|(k, _)| k.clone()).collect::<HashSet<String>>();

    let data_collection_dsl = populate_from_external(
        "https/harness-internal-read%40harness.jfrog.io/artifactory/harness-internal",
        "io/harness/cv",
        "data-collection-dsl",
        "0.18-RELEASE",
        Some("CV".to_string()),
    );

    let mut result: HashMap<String, JavaModule> = modules
        .par_iter()
        .map(|name| {
            (
                name.clone(),
                populate_from_bazel(name, module_rules.get(name).unwrap(), &modules),
            )
        })
        .collect::<HashMap<String, JavaModule>>();

    result.insert("data-collection-dsl".to_string(), data_collection_dsl);

    result
}

fn class_for_prefix(path: &String, prefix: &str) -> Option<String> {
    let pos = path.find(prefix);
    if pos.is_some() {
        Some(
            path.chars()
                .skip(pos.unwrap() + prefix.len())
                .take(path.len() - prefix.len() - pos.unwrap() - ".java".len())
                .map(|ch| if ch == '/' { '.' } else { ch })
                .collect(),
        )
    } else {
        None
    }
}

fn class(path: &String) -> String {
    let prefixes = [
        "/src/main/java/",
        "/src/test/java/",
        "/src/generated/java/",
        "/src/supporter-test/java/",
        "/src/abstract/java",
    ];
    let cls = prefixes
        .iter()
        .map(|&prefix| class_for_prefix(path, prefix))
        .filter(|cls| cls.is_some())
        .map(|cls| cls.unwrap())
        .nth(0);

    cls.expect(&format!("{} did not fit any prefix", path))
}

fn populate_srcs(name: &str, dependencies: &MultiMap<String, String>) -> HashMap<String, JavaClass> {
    let mut split = name.split(":");
    let chunk = split.next().unwrap();
    let module_type = split.next().unwrap();
    let prefix = &format!("{}:", chunk);
    let directory = &format!("{}/", chunk.strip_prefix("//").unwrap());

    let filename = &format!(
        "{}/.aeriform/{}_aeriform_sources.txt",
        BAZEL_BAZEL_GENFILES_DIR.as_str(),
        name.replace("/", "").replace(":", "!")
    );

    let sources = fs::read_to_string(filename).expect("");

    sources
        .lines()
        .par_bridge()
        .map(|line| line.to_string())
        .map(|line| line.replace(prefix, directory))
        .map(|line| (class(&line), line))
        .map(|tuple| {
            let (package, target_module, break_dependencies_on, team, deprecated) =
                populate_internal_info(&tuple.1, module_type);
            let class_dependencies = class_dependencies(&tuple.0, &dependencies);
            (
                tuple.0.clone(),
                JavaClass {
                    name: tuple.0,
                    package: package,
                    location: tuple.1,
                    dependencies: class_dependencies,
                    target_module: target_module,
                    team: team,
                    break_dependencies_on: break_dependencies_on,
                    deprecated: deprecated,
                },
            )
        })
        .collect::<HashMap<String, JavaClass>>()
}

fn populate_module_dependencies(name: &str, modules: &HashSet<String>) -> HashSet<String> {
    let filename = &format!(
        "{}/.aeriform/{}_aeriform_dependencies.txt",
        BAZEL_BAZEL_GENFILES_DIR.as_str(),
        name.replace("/", "").replace(":", "!")
    );

    let depedencies = fs::read_to_string(filename).expect(&format!("{} is missing.", filename));

    depedencies
        .lines()
        .par_bridge()
        .map(|line| line.to_string())
        .filter(|name| modules.contains(name.as_str()))
        .collect::<HashSet<String>>()
}

fn dependency_class(line: &String) -> (String, String) {
    let re = Regex::new(r"^\s+([^ ]+)\s+->\s+([^ ]+).*").unwrap();

    let captures = re.captures(line).expect(line);

    let mtch1 = captures.get(1).expect("");
    let mtch2 = captures.get(2).expect("");

    (
        mtch1.as_str().split('$').nth(0).unwrap().to_string(),
        mtch2.as_str().split('$').nth(0).unwrap().to_string(),
    )
}

fn is_harness_class(class: &String) -> bool {
    class.starts_with("io.harness.") || class.starts_with("software.wings.")
}

const GOOGLE_PROTO_BASE_CLASSES: &'static [&'static str] = &[
    "com.google.protobuf.UnknownFieldSet",
    "com.google.protobuf.ProtocolMessageEnum",
    "io.grpc.stub.AbstractStub",
];

fn populate_dependencies(name: &String) -> (MultiMap<String, String>, HashSet<String>) {
    let all = target_dependencies(name);

    (
        all.iter()
            .filter(|tuple| is_harness_class(&tuple.0))
            .filter(|tuple| is_harness_class(&tuple.1))
            .map(|tuple| (tuple.0.clone(), tuple.1.clone()))
            .collect::<MultiMap<String, String>>(),
        all.iter()
            .filter(|&tuple| GOOGLE_PROTO_BASE_CLASSES.iter().any(|&base| base.eq(&tuple.1)))
            .map(|tuple| tuple.0.clone())
            .filter(|class| is_harness_class(class))
            .collect::<HashSet<String>>(),
    )
}

fn jar_dependencies(jar: &str) -> Vec<(String, String)> {
    if !Path::new(jar).exists() {
        panic!(format!("Jar file {} does not exist", jar));
    }

    let output = Command::new("jdeps")
        .args(&["-v", jar])
        .output()
        .expect("not able to run bazel");

    if !output.status.success() {
        panic!("Command executed with failing error code");
    }

    let result = String::from_utf8(output.stdout).unwrap();
    //println!("jar: {:?}", result);

    result
        .lines()
        .par_bridge()
        .map(|line| line.to_string())
        .filter(|line| line.starts_with("   "))
        .map(|s| dependency_class(&s))
        .filter(|tuple| !tuple.0.eq(&tuple.1))
        .collect()
}

fn target_dependencies(name: &str) -> Vec<(String, String)> {
    let filename = &format!(
        "{}/.aeriform/{}_aeriform_jdeps.txt",
        BAZEL_BAZEL_GENFILES_DIR.as_str(),
        name.replace("/", "").replace(":", "!")
    );

    let sources = fs::read_to_string(filename).expect("");

    sources
        .lines()
        .par_bridge()
        .map(|line| line.to_string())
        .filter(|line| line.starts_with("   "))
        .map(|s| dependency_class(&s))
        .filter(|tuple| !tuple.0.eq(&tuple.1))
        .collect()
}

lazy_static! {
    static ref BAZEL_BAZEL_GENFILES_DIR: String = String::from_utf8(
        Command::new("bazel")
            .args(&["info", "bazel-genfiles"])
            .output()
            .unwrap()
            .stdout
    )
    .unwrap()
    .trim()
    .to_string();
    static ref BAZEL_OUTPUT_BASE_DIR: String = String::from_utf8(
        Command::new("bazel")
            .args(&["info", "output_base"])
            .output()
            .unwrap()
            .stdout
    )
    .unwrap()
    .trim()
    .to_string();
}

fn populate_from_bazel(name: &String, rule: &String, modules: &HashSet<String>) -> JavaModule {
    let mut split = name.split(":");
    let directory = split.next().unwrap().strip_prefix("//").unwrap().to_string();
    let target_name = split.next().unwrap();
    let prefix = if rule.eq("java_library") { "lib" } else { "" };

    let jar = format!(
        "{}/{}/{}{}.jar",
        BAZEL_BAZEL_GENFILES_DIR.as_str(),
        directory,
        prefix,
        target_name
    );

    let module_dependencies = populate_module_dependencies(name, modules);

    let (dependencies, maybe_protos) = populate_dependencies(name);

    let mut srcs = populate_srcs(&name, &dependencies);
    // println!("{:?}", srcs);

    maybe_protos.iter().for_each(|class| {
        if !srcs.contains_key(class) {
            srcs.insert(class.to_string(), external_class(class, &dependencies, None));
        }
    });

    JavaModule {
        name: name.clone(),
        deprecated: is_deprecated(name),
        index: directory
            .chars()
            .take(3)
            .collect::<String>()
            .parse::<f32>()
            .expect("the model index was ot resolved")
            + index_fraction(name),
        directory: directory,
        jar: jar,
        srcs: srcs,
        dependencies: module_dependencies,
    }
}

fn is_deprecated(name: &String) -> bool {
    name.contains("/400-rest:")
}

fn index_fraction(name: &String) -> f32 {
    if name.ends_with(":tests") {
        0.0
    } else if name.ends_with(":supporter-test") {
        0.1
    } else if name.ends_with(":module") {
        0.2
    } else if name.ends_with(":abstract-module") {
        0.3
    } else {
        panic!("Unknown module type {}", name);
    }
}

fn populate_from_external(
    artifactory: &str,
    package: &str,
    name: &str,
    version: &str,
    team: Option<String>,
) -> JavaModule {
    let jar = format!(
        "{}/external/maven_harness/v1/{}/{}/{}/{}/{}-{}.jar",
        BAZEL_OUTPUT_BASE_DIR.as_str(),
        artifactory,
        package,
        name,
        version,
        name,
        version
    );

    let all = jar_dependencies(&jar);
    //println!("{} {:?}", name, dependencies);

    let dependencies = all
        .iter()
        .filter(|tuple| is_harness_class(&tuple.0))
        .filter(|tuple| is_harness_class(&tuple.1))
        .map(|tuple| (tuple.0.clone(), tuple.1.clone()))
        .collect::<MultiMap<String, String>>();

    let srcs = all
        .iter()
        .filter(|tuple| is_harness_class(&tuple.0))
        .map(|tuple| {
            (
                tuple.0.to_string(),
                external_class(&tuple.0, &dependencies, team.clone()),
            )
        })
        .collect();

    JavaModule {
        name: name.to_string(),
        deprecated: false,
        index: 1000.0,
        directory: "n/a".to_string(),
        jar: jar,
        srcs: srcs,
        dependencies: HashSet::new(),
    }
}
