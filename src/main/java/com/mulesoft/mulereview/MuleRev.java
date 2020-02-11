package com.mulesoft.mulereview;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


@Mojo(name = "MuleRev", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class MuleRev extends AbstractMojo {

	/**
	 * XML document absolute path
	 */
	@Parameter(required = true)
	private String sourceXmlFilePathDirectory;
	
	@Parameter(required = true)
	private String sourceConfigFilePathDirectory;
	
	@Parameter(required = true)
	private String applicationName;
	private int flowsCount = 0;

	private List<String> majorIssue = new ArrayList<String>();
	private List<String> criticalIssue = new ArrayList<String>();
	private List<String> minorIssue = new ArrayList<String>();
	StringBuilder flowNames = new StringBuilder();

	final Set<String> uniqueConnectors = new HashSet<String>();
	final Set<String> duplicateConnectors = new HashSet<String>();
	enum env 
	{ 
	    dev, test, stage, prod; 
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		
		
		File muleYAMlFolder = new File(sourceConfigFilePathDirectory);
		
		for(File yamlFile : muleYAMlFolder.listFiles()) {
			if (yamlFile.isFile()) {
				EnumSet.allOf(env.class)
	            .forEach(muleEnv -> {
	            	File muleYaml = new File(sourceConfigFilePathDirectory+"/"+applicationName+"-"+muleEnv+".yaml");
	            	try {
						if(muleYaml.createNewFile()) {
							getLog().info("YAML file created for env : "+ muleEnv + " filename "+"-"+muleYaml.getName());
						}
						else {
							getLog().debug(applicationName+"-"+muleEnv+".yaml File already exist");
						}
					} catch (IOException e) {
						/*e.printStackTrace();*/
						getLog().info("IO exception occured and skipped the file for further processsing : " + e.getMessage());
					}
	            });
			}
		}
		File muleConfigFolder = new File(sourceXmlFilePathDirectory);
		File[] listOfFiles = muleConfigFolder.listFiles();
		boolean isGlobalFile = new File(muleConfigFolder, "global").exists();
		getLog().debug("is global file : " + isGlobalFile);
		if (!isGlobalFile) {
			criticalIssue.add("Critical - Global configuration file for common connectors is missing - bad practice");
		}
		for (File file : listOfFiles) {
			if (file.isFile()) {
				getLog().debug("Mule config file names : " + file.getName() + "path : " + sourceXmlFilePathDirectory
						+ "/" + file.getName());
				String xmlContent;

				try {
					xmlContent = FileUtils.readFileToString(new File(sourceXmlFilePathDirectory + "/" + file.getName()),
							"UTF-8");
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder;
					builder = factory.newDocumentBuilder();
					Document document;
					document = builder.parse(new StringBufferInputStream(xmlContent));
					document.getDocumentElement().normalize();
					/*Element root = document.getDocumentElement();*/
					scanMuleConfigFiles(document, file.getName());
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		getLog().info(
				"***************************************** Total number of config files ******************************************* : "
						+ listOfFiles.length);
		/* getLog().info("Flow names  : " + flowNames); */
		getLog().info(
				"***************************************** Total number of Flows ************************************************** : "
						+ flowsCount);
		getLog().info(
				"***************************************** Total number of Critical issue ***************************************** : "
						+ criticalIssue.size());
		getLog().info(
				"***************************************** Total number of Major issue ******************************************** : "
						+ majorIssue.size());
		/*getLog().info("Below are the Major issue details");
		majorIssue.forEach(details -> getLog().info(details));*/
		getLog().info(
				"***************************************** Total number of Minor issue ******************************************** : "
						+ minorIssue.size());
		getLog().info(
				"************************************Below are the Critical issue detais ****************************************** : ");
		List<String> combinedissueList = Stream.of(criticalIssue, majorIssue,minorIssue)
                .flatMap(list -> list.stream())
                .collect(Collectors.toList());
		combinedissueList.forEach(details -> getLog().info(details));
		if (majorIssue.size() > 5 || criticalIssue.size() > 0) {
			getLog().info("Mojo exception is thrown");
			throw new MojoExecutionException(
					"Build failed as there is more than 1 Critical issue or 5 Major issues , Please check the MAVEN logs");

		}
	}

	private void scanMuleConfigFiles(Document document, String fileName) throws MojoExecutionException {

		getLog().info("Started scanning : " + fileName);
		if (fileName.contains("global")) {
			getLog().debug(fileName + " is a globals configuration file , should not contain any flows");
			getDuplicateConnectors(document, fileName);
		} else if (fileName.contains("error")) {
			getLog().debug("Global exception handler should be defined here");
		} else {
			getLog().debug(fileName + " is a mule configuration file , should not contain any connector configuration");
			getDuplicateConnectors(document, fileName);
			/* scanFlows(nListFlow, fileName); */
		}
		getLog().info("Completed scanning : " + fileName);
		/*
		 * getLog().info("Total number of Critical issue : " + critical);
		 * getLog().info("Total number of Major issue : " + major);
		 * getLog().info("Total number of Minor issue : " + minor);
		 */

	}
	// Rule to check the duplicate connectors in the XML file and check for Security
	// implementations
	private void getDuplicateConnectors(Document document, String fileName) {
		NodeList globalChildElements = document.getElementsByTagName("mule");
		boolean isSecured = true;
		boolean isGlobalConfig = fileName.toLowerCase().contains("global") ? true : false;
		if (globalChildElements.item(0).hasChildNodes()) {
			NodeList childNodes = globalChildElements.item(0).getChildNodes();
			for (int index = 0; index < childNodes.getLength(); index++) {
				if (childNodes.item(index).getNodeType() == Node.ELEMENT_NODE) {

					if (!isGlobalConfig && !childNodes.item(index).getNodeName().toLowerCase().contains("flow")) {
						majorIssue.add("Major : Connector/Global configuration " + childNodes.item(index).getNodeName()
								+ " are seen in Mule config, please move it to -Globals.xml from file : " + fileName);
					} else if (!isGlobalConfig) {
						checkForErrorHandlerinFlow(childNodes.item(index), fileName);
					} else {
						isSecured = childNodes.item(index).getNodeName().contains("security") ? true : false;
						if (!uniqueConnectors.add(childNodes.item(index).getNodeName())) {
							duplicateConnectors.add(childNodes.item(index).getNodeName());
						}
					}
				}
			}
			duplicateConnectors.stream().forEach(
					connector -> majorIssue.add("Major : Duplicate connectors configuration : " + connector + " found"));
			if (!isSecured) {
				majorIssue.add("Major : Secured property place holder is not implemented in the file : " + fileName);
			}
		} else {
			majorIssue.add("Major : configuration XML file is empty : " + fileName);
		}
	}

	private void checkForErrorHandlerinFlow(Node node, String fileName) {
		String flowName = null;
		boolean isErrorHandler = false;
		getLog().debug("Node value : " + node.getNodeName());
		if (node.hasAttributes()) {
			for (int index = 0; index < node.getAttributes().getLength(); index++) {
				if (node.getAttributes().item(index).getNodeName().toLowerCase().contains("name")) {
					flowName = node.getAttributes().item(index).getNodeValue();
					flowsCount = flowsCount+1;
					getLog().debug("Flow name : " + node.getAttributes().item(index).getNodeValue());
				}
			}
		}
		if (node.hasChildNodes()) {

			for (int index = 0; index < node.getChildNodes().getLength(); index++) {
				if (node.getChildNodes().item(index).getNodeType() == Node.ELEMENT_NODE
						&& (node.getChildNodes().item(index).getNodeName().toLowerCase().contains("try")
								|| node.getChildNodes().item(index).getNodeName().toLowerCase().contains("error"))) {
					getLog().debug("Elements name : " + node.getChildNodes().item(index).getNodeName());
					isErrorHandler = true;
				}
				checkForDefaultName(node.getChildNodes().item(index),flowName);
			}
		}

		if (!isErrorHandler) {
			majorIssue
					.add("Critical : There should be atleast one error handler or try catch for flow : " + flowName + "If it is a private flow, with out error handler. Please make it sub flow");
		}
	}

	private void checkForDefaultName(Node node, String flowName) {
		
	// Checking for choice and for each and other default names
		
		if(node.hasChildNodes()) {
			if(node.hasAttributes()) {
				checkElementName(node,flowName);
			}
			for(int elementIndex = 0; elementIndex < node.getChildNodes().getLength(); elementIndex++) {
				checkElementName(node,flowName);
			}
			
		} else if (node.hasAttributes()){
			checkElementName(node,flowName);
			}
		

	}
	
	private void checkDefaultnamesForElement(Node node, String flowName, String attrName) {
		
		for(int attrIndex =0 ; attrIndex < node.getAttributes().getLength() ; attrIndex ++) {
			if(node.getAttributes().item(attrIndex).getNodeName().equals("doc:name") && node.getAttributes().item(attrIndex).getNodeValue().toLowerCase().equals(attrName.toLowerCase())) {
				getLog().debug("Checking the duplicate elements for " + attrName + "value : " + node.getAttributes().item(attrIndex).getNodeName() + ":" + node.getAttributes().item(attrIndex).getNodeValue().toLowerCase());
				minorIssue.add("Minor : Default name for the " + attrName + ", please define the valid name. Flow name - " + flowName );
			}
		}
	}
	
	private void checkElementName(Node node,String flowName) {
		String removeEETag = node.getNodeName().toLowerCase().contains(":") ? node.getNodeName().toLowerCase().split(":")[1] : node.getNodeName().toLowerCase();
		getLog().debug("Attribute name  removeEETag : " + removeEETag);
		switch(removeEETag) {
		case "logger" : checkDefaultnamesForElement(node,flowName,"Logger");
		case "transform" : checkDefaultnamesForElement(node,flowName,"Transform Message");
		case "choice" : checkDefaultnamesForElement(node,flowName,"choice");
		case "stored-procedure" : checkDefaultnamesForElement(node,flowName,"Stored procedure");
	}
	}
	
}
