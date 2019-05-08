/*-
 * Copyright 2019, Darran Kartaschew.
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

package com.github.dkartaschew;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Rebuild defined artifacts or JARs, updating the included manifest with
 * details from the provided supplemental manifest.
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageMOJO extends AbstractMojo {

	/**
	 * Default buffer size for file IO
	 */
	private final static int BUFFER_SIZE = 32768;

	/**
	 * Target location to store files.
	 */
	@Parameter(defaultValue = "${project.build.directory}")
	private File outputDirectory;

	/**
	 * The location of the local maven repository.
	 */
	@Parameter(defaultValue = "${settings.localRepository}")
	private File localRepository;

	/**
	 * List of all artifacts/JAR files to update.
	 * <p>
	 * Each artifact <b>must</b> define either a local maven artifact
	 * ({@code artifact}) or a JAR File ({@code jarFile}) and a supplemental
	 * manifest file.
	 * <p>
	 * Artifact is defined as:
	 * 
	 * <pre>
&lt;artifacts&gt;
&lt;artifact&gt;
&nbsp;&nbsp;&lt;artifact&gt;org.apache.maven:maven-plugin-api:3.5.0&lt;/artifact&gt;
&nbsp;&nbsp;&lt;jarFile&gt;org.apache.maven.maven-plugin-api-3.5.0.jar&lt;/jarFile&gt;
&nbsp;&nbsp;&lt;manifestFile&gt;src/manifests/maven-plugin-api.mf&lt;/manifestFile&gt;
&nbsp;&nbsp;&lt;mode&gt;merge&lt;/mode&gt;
&nbsp;&nbsp;&lt;publishArtifact&gt;false&lt;/publishArtifact&gt;
&lt;/artifact&gt;
...
&lt;/artifacts&gt;
	 * </pre>
	 * <p>
	 * {@code artifact} defines a JAR file located in the local maven repository. The
	 * format definition uses Apache Buildr notation. (This is effectively
	 * {@code groupId:artifactId:version}).
	 * <p>
	 * {@code jarFile} defines a local JAR file.
	 * <p>
	 * {@code manifestFile} defines the supplemental manifest file that will
	 * update the manifest in the JAR file. The provided manifest file must
	 * conform to the <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#JAR_Manifest">JAR Manifest specification</a>.
	 * <p>
	 * {@code mode} and {@code publishArtifact} are optional.
	 * <p>
	 * {@code mode} can be set to "merge" or "overwrite". "merge" will instruct
	 * the plugin to merge the contents of the original and supplement manifest, and
	 * "overwrite" will replace the original with the supplement manifest. 
	 * (Defaults to "merge"). 
	 * <p>
	 * {@code publishArtifact} can be set to "true" or "false". This setting
	 * instructs the plugin to republish an updated JAR back into the local
	 * maven repository post processing. This setting has no effect if
	 * the JAR file being updated did not originate from the local maven 
	 * repository. (Defaults to "false").
	 */
	@Parameter(required = true)
	private List<ArtifactDefinition> artifacts;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		/*
		 * Check to ensure we have something to work on, if not, give a warning.
		 */
		if (artifacts == null || artifacts.isEmpty()) {
			getLog().warn("No artifacts defined, skipping...");
			return;
		}
		/*
		 * Process each element, skipping Signed JAR, otherwise throw appropriate
		 * exception.
		 */
		for (ArtifactDefinition def : artifacts) {
			try {
				process(def);
			} catch (SignedJARException error) {
				getLog().warn(error);
			} catch (IOException | IllegalStateException error) {
				getLog().error(error);
				throw new MojoFailureException(error.getMessage(), error);
			} catch (Throwable error) {
				getLog().error(error);
				throw new MojoExecutionException(error.getMessage(), error);
			}
		}
	}

	/**
	 * Process the given definition
	 * 
	 * @param def The definition to process
	 * @throws IOException           Error occurred processing the JAR file
	 * @throws SignedJARException    The JAR File is signed.
	 * @throws IllegalStateException The definition is invalid.
	 */
	void process(ArtifactDefinition def) throws IOException, SignedJARException, IllegalStateException {
		// Validate input.
		if (def == null) {
			throw new IllegalStateException("Missing definition");
		}
		if (!def.isValidState()) {
			throw new IllegalStateException("Missing artifact or JAR File definition");
		}
		if (def.getManifestFile() == null) {
			throw new IllegalStateException("Missing manifest definition");
		}

		// Load the new manifest
		Manifest newManifest;
		try (FileInputStream input = new FileInputStream(def.getManifestFile())) {
			newManifest = new Manifest(input);
		}

		// Start by copying the contents...
		Path zipFile;
		Path outFile;
		try (JarFile jarFile = getSourceFile(def)) {
			zipFile = Paths.get(jarFile.getName());
			getLog().info("Processing : " + zipFile.toString());

			// Check the existing manifest for signed jar entries.
			Manifest jarFileManifest = jarFile.getManifest();
			if (!jarFileManifest.getEntries().isEmpty()) {
				throw new SignedJARException(zipFile.getFileName().toString() + " appears to be signed, skipping.");
			}

			// Now create a new output file and copy the contents over.
			outFile = outputDirectory.toPath().resolve(zipFile.getFileName());
			// Ensure the target location exists...
			Files.createDirectories(outputDirectory.toPath());
			try (ZipOutputStream outZipContainer = new ZipOutputStream(new FileOutputStream(outFile.toFile()))) {
				if (jarFile.getComment() != null) {
					outZipContainer.setComment(jarFile.getComment());
				}
				// Copy all entries except the manifest
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry je = entries.nextElement();
					if (!je.getName().equalsIgnoreCase(JarFile.MANIFEST_NAME)) {
						ZipEntry newEntry = clone(je);
						outZipContainer.putNextEntry(newEntry);
						byte[] buffer = new byte[BUFFER_SIZE];
						try (InputStream in = jarFile.getInputStream(je)) {
							int len;
							while ((len = in.read(buffer)) > 0) {
								outZipContainer.write(buffer, 0, len);
							}
						}
						outZipContainer.closeEntry();
					}
				}
				outZipContainer.flush();
				// Now process the manifest.
				ZipEntry ze = new ZipEntry(JarFile.MANIFEST_NAME);
				outZipContainer.putNextEntry(ze);
				if (def.getMode().equalsIgnoreCase("overwrite")) {
					// overwrite
					newManifest.write(outZipContainer);
				} else {
					// merge
					Manifest m = new Manifest(jarFileManifest);
					m.getMainAttributes().putAll(newManifest.getMainAttributes());
					m.write(outZipContainer);
				}
				outZipContainer.closeEntry();

				// Finalise the output file...
				outZipContainer.finish();
			}
		}
		// And lastly, if from local repo, overwrite source...
		if (def.getJarFile() == null && def.isPublishArtifact()) {
			Files.copy(outFile, zipFile, StandardCopyOption.REPLACE_EXISTING);
			// Update sha1 signature. (TODO: add others if necessary).
			try {
				MessageDigest sha1 = MessageDigest.getInstance("SHA1");
				byte[] buffer = new byte[BUFFER_SIZE];
				try (InputStream in = new FileInputStream(zipFile.toFile())) {
					int len;
					while ((len = in.read(buffer)) > 0) {
						sha1.update(buffer, 0, len);
					}
				}
				File digestFile = new File(zipFile.toString() + ".sha1");
				try (FileOutputStream out = new FileOutputStream(digestFile)) {
					out.write(byteArrayToHex(sha1.digest()).getBytes(StandardCharsets.UTF_8));
				}
			} catch (NoSuchAlgorithmException e) {
				getLog().error(e);
			}
		}
	}

	/**
	 * Clone the JarEntry as a ZipEntry
	 * 
	 * @param je The jarfile enry
	 * @return A clone of the jarFile entry.
	 */
	private ZipEntry clone(JarEntry je) {
		/*
		 * Don't use the ZipEntry(ZipEntry e) constructor, as this will copy things we
		 * don't want copied. Therefore do it manually.
		 */
		ZipEntry ze = new ZipEntry(je.getName());
		if (je.getComment() != null)
			ze.setComment(je.getComment());
		if (je.getCreationTime() != null)
			ze.setCreationTime(je.getCreationTime());
		if (je.getLastAccessTime() != null)
			ze.setLastAccessTime(je.getLastAccessTime());
		if (je.getLastModifiedTime() != null)
			ze.setLastModifiedTime(je.getLastModifiedTime());
		if (je.getExtra() != null)
			ze.setExtra(je.getExtra());
		ze.setSize(je.getSize());
		if (je.getMethod() == ZipOutputStream.STORED) {
			ze.setCrc(je.getCrc());
			ze.setCompressedSize(je.getCompressedSize());
		}
		ze.setTime(je.getTime());
		ze.setMethod(je.getMethod());
		return ze;
	}

	/**
	 * Get the source file.
	 * 
	 * @param def The artifact definition
	 * @return The Jar File
	 * @throws IOException Opening the source failed.
	 */
	private JarFile getSourceFile(ArtifactDefinition def) throws IOException {
		if (def.getJarFile() != null) {
			return new JarFile(def.getJarFile());
		}
		String[] artifact = def.getArtifact().split(":");
		if (artifact.length != 3) {
			throw new IllegalStateException("Artifact definition '" + def.getArtifact() + "' is invalid");
		}
		Path path = localRepository.toPath();
		try {
			String groupID = artifact[0];
			String artifactID = artifact[1];
			String version = artifact[2];

			String[] groupParts = groupID.split("\\.");
			for (String g : groupParts) {
				path = path.resolve(g);
			}
			path = path.resolve(artifactID);
			path = path.resolve(version);
			path = path.resolve(artifactID + "-" + version + ".jar");
		} catch (NullPointerException | InvalidPathException e) {
			throw new IOException("Unable to locate artifact '" + def.getArtifact() + "'");
		}
		return new JarFile(path.toFile());
	}

	/**
	 * Convert the byte array to a hex string
	 * 
	 * @param a The byte array
	 * @return A string representation.
	 */
	private String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder(a.length * 2);
		for (byte b : a)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}

	/**
	 * Get the output/target directory
	 * 
	 * @return The output directory.
	 */
	File getOutputDirectory() {
		return outputDirectory;
	}

	/**
	 * Set the output/target directory
	 * 
	 * @param outputDirectory The output directory to use.
	 */
	void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	/**
	 * Get the local maven repository location
	 * 
	 * @return The local maven repository location
	 */
	File getLocalRepository() {
		return localRepository;
	}

	/**
	 * Set the local maven repository location
	 * 
	 * @param localRepository The local maven repository location
	 */
	void setLocalRepository(File localRepository) {
		this.localRepository = localRepository;
	}

	/**
	 * Get a list of artifacts to update
	 * 
	 * @return The list of artifacts to update.
	 */
	List<ArtifactDefinition> getArtifacts() {
		return artifacts;
	}

	/**
	 * Set the list of artifacts to update
	 * 
	 * @param artifacts The list of items to update
	 */
	void setArtifacts(List<ArtifactDefinition> artifacts) {
		this.artifacts = artifacts;
	}

}
