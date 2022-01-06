/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.rule;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MetadataPersister {
  private final String outputFolder;

  public MetadataPersister(String outputFolder) {
    this.outputFolder = outputFolder;
  }

  public void persist(Map<String, List<TestMetadata>> testMetadataMap) throws Exception {
    for (Entry<String, List<TestMetadata>> classMetadataList : testMetadataMap.entrySet()) {
      String className = classMetadataList.getKey();
      File file = getSurefireXmlFileFor(className);
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = factory.newDocumentBuilder();
      Document document = documentBuilder.parse(file);
      addMetadataToDocument(document, classMetadataList.getValue());
      save(document, file);
    }
  }

  private void addMetadataToDocument(Document document, List<TestMetadata> classMetadataList)
      throws XPathExpressionException {
    for (TestMetadata testMetadata : classMetadataList) {
      String methodName = testMetadata.getMethodName();
      Element node = getNodeForMethod(document, methodName);
      String developer = testMetadata.getDeveloper();
      UserInfo devInfo = OwnerRule.findDeveloper(developer);
      node.setAttribute("email", devInfo.getEmail());
      node.setAttribute("team", devInfo.getTeam());
    }
  }

  private Element getNodeForMethod(Document document, String testCaseName) throws XPathExpressionException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    XPathExpression expression = xPath.compile("//testcase[@name='" + testCaseName + "']");
    return (Element) expression.evaluate(document, XPathConstants.NODE);
  }

  private void save(Document document, File file) throws TransformerException {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    DOMSource source = new DOMSource(document);
    StreamResult result = new StreamResult(file);
    transformer.transform(source, result);
  }

  private File getSurefireXmlFileFor(String className) throws IOException {
    return new File(outputFolder, getSurefireXmlFileNameFor(className)).getCanonicalFile();
  }

  private String getSurefireXmlFileNameFor(String className) {
    return "TEST-" + className + ".xml";
  }
}
