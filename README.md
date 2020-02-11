# mule-review-plugin
Mule code review plugin is developed using maven MOJO.

Plugin checks for below rules.
1. Duplicate connectors in the configuration file.
2. Atleast one Exception or try catch for public or private flows.
3. Default names for choice, Dataweave , logger etc...
4. Globals configuration file for common connectors.

Planned enhancement for more rules independent from the code as this is the initial commit.

Steps to use plugin.

Go to root of the mulerev project and run mvn clean install.
Add plugin details in mule project and run mvn clean intsall/package/build

<plugin>
				<groupId>com.mulesoft.review</groupId>
				<artifactId>mulerev</artifactId>
				<version>3.1.8-SNAPSHOT</version>
				<executions>
					<execution>
						<configuration>
							<sourceXmlFilePathDirectory>${project.basedir}/src/main/mule/</sourceXmlFilePathDirectory>
							<sourceConfigFilePathDirectory>${project.basedir}/src/main/resources/</sourceConfigFilePathDirectory>
							<applicationName>${name}</applicationName>
						</configuration>
						<goals>
							<goal>MuleRev</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
