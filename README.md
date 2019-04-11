# Java Manifest Update Plugin

A simple maven plugin to update a JAR manifest in an
existing JAR or artifact located in a local maven
repository.

## License

The plugin is licensed under the Apache 2.0 license.

## Installation

To install the plugin, simply use: `$ mvn clean install`

## Usage

In the POM use:

```
<build>
		<plugins>
			<plugin>
				<groupId>com.github.dkartaschew</groupId>
				<artifactId>manifest-update-maven-plugin</artifactId>
				<version>1.0.0</version>
				<executions>
					<execution>
						<phase>package</phase>
						<goals>
							<goal>package</goal>
						</goals>
					</execution>
				</executions>
				<configuration>
					<artifacts>
						<artifact>
							<publishArtifact>false</publishArtifact>
							<artifact>org.apache.maven:maven-plugin-api:3.5.0</artifact>
							<manifestFile>maven-plugin-api.mf</manifestFile>
							<mode>merge</mode>
						</artifact>
						...
					</artifacts>
				</configuration>
			</plugin>
			...
		</plugins>
	</build>
```

This will bind the action to the package:package phase.

The Artifact defines what JAR or Maven Artifact to update. The available 
fields are:

1. artifact - The name of the artifact in the local maven repository, 
 using Apache Buildr notation. (This is simply groupId:artifactId:version).
2. jarFile - The name of the JAR File to update.
3. manifestFile - The name of the Manifest File which will update or 
overwrite the manifest in the JAR file
4. mode - 'merge' to merge the contents, or 'overwrite' to replace the entire
manifest.
5. publishArtifact - 'true' to publish the artifact back into the local maven 
repository, or 'false' to leave in the 'target' folder. This setting only applies
if the source is from the local maven repository.

Further examples are in the /examples folder.

## Notes

1. The plugin when installing back into the local repository will update
 the sha1 hash as well. If other digests are in use, these need to manually
 updated. (TODO: Automate other digests).
2. The manifest file that provides updates must be a valid manifest file.
3. Signed JARs will be skipped and NOT processed.

