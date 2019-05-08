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

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Item / Artifact update definition
 */
public class ArtifactDefinition {

	/**
	 * The JAR File to update
	 */
	@Parameter
	private File jarFile;

	/**
	 * The name of the artifact located in the local maven repository. This should
	 * be in Apache Buildr format.
	 */
	@Parameter
	private String artifact;

	/**
	 * The name of the manifest file to use as the source to update.
	 */
	@Parameter(required = true)
	private File manifestFile;

	/**
	 * The update mode. (either {@code merge} or {@code overwrite}).
	 */
	@Parameter(defaultValue = "merge")
	private String mode;

	/**
	 * Flag to indicate if to republish an artifact back to the local maven
	 * repository.
	 */
	@Parameter(defaultValue = "false")
	private boolean publishArtifact;

	/**
	 * The name of the JAR File to update.
	 * 
	 * @return The name of the JAR File to update or {@code null} if not set.
	 */
	public File getJarFile() {
		return jarFile;
	}

	/**
	 * Set the name of the JAR File to update.
	 * 
	 * @param jarFile The name of the JAR File to update, or {@code null} to unset.
	 */
	public void setJarFile(File jarFile) {
		this.jarFile = jarFile;
	}

	/**
	 * Get the name of the artifact to update.
	 * 
	 * @return The name of the artifact to update.
	 */
	public String getArtifact() {
		return artifact;
	}

	/**
	 * Set the name of the artifact to update. This name should be in apache buildr
	 * format. (groupId:artifactId:version).
	 * <p>
	 * This method does <b>not</b> verify artifact
	 * 
	 * @param artifact The name of the artifact to update.
	 */
	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}

	/**
	 * Get the manifest file that will be used as the source to update the manifest
	 * in the JAR/artifact.
	 * 
	 * @return The manifest file.
	 */
	public File getManifestFile() {
		return manifestFile;
	}

	/**
	 * Set the manifest file that will be used as the source to update the manifest.
	 * 
	 * @param manifestFile The file.
	 */
	public void setManifestFile(File manifestFile) {
		this.manifestFile = manifestFile;
	}

	/**
	 * Get the update mode.
	 * 
	 * @return The update mode.
	 */
	public String getMode() {
		if (mode == null) {
			return "merge";
		}
		return mode;
	}

	/**
	 * Set the update mode.
	 * 
	 * @param mode The update mode, or {@code null} to reset to default.
	 */
	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * Should the result be published back into the local maven repository.
	 * 
	 * @return TRUE if the updated artifact should be published.
	 */
	boolean isPublishArtifact() {
		return publishArtifact;
	}

	/**
	 * Set if the artifact that was updated should be published back into the local
	 * maven repository.
	 * 
	 * @param publishArtifact TRUE to publish.
	 */
	void setPublishArtifact(boolean publishArtifact) {
		this.publishArtifact = publishArtifact;
	}

	/**
	 * Does this artifact have enough information to proceed.
	 * <p>
	 * This method does not check to see if items set are accessibly/readable/valid,
	 * just that values have been set.
	 * 
	 * @return TRYE if all required values have been set.
	 */
	public boolean isValidState() {
		return jarFile != null || (artifact != null && !artifact.trim().isEmpty());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((artifact == null) ? 0 : artifact.hashCode());
		result = prime * result + ((jarFile == null) ? 0 : jarFile.hashCode());
		result = prime * result + ((manifestFile == null) ? 0 : manifestFile.hashCode());
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
		result = prime * result + (publishArtifact ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArtifactDefinition other = (ArtifactDefinition) obj;
		if (artifact == null) {
			if (other.artifact != null)
				return false;
		} else if (!artifact.equals(other.artifact))
			return false;
		if (jarFile == null) {
			if (other.jarFile != null)
				return false;
		} else if (!jarFile.equals(other.jarFile))
			return false;
		if (manifestFile == null) {
			if (other.manifestFile != null)
				return false;
		} else if (!manifestFile.equals(other.manifestFile))
			return false;
		if (mode == null) {
			if (other.mode != null)
				return false;
		} else if (!mode.equals(other.mode))
			return false;
		if (publishArtifact != other.publishArtifact)
			return false;
		return true;
	}

}
