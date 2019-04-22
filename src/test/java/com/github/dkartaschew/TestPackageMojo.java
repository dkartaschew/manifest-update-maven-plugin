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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.Test;

public class TestPackageMojo {

	private final Path TMP = Paths.get(System.getProperty("java.io.tmpdir"));
	private final Path MVN_REPO = Paths.get(System.getProperty("user.home"), ".m2", "repository");

	@Test(expected = IllegalStateException.class)
	public void testNullEntry() throws IllegalStateException, IOException, SignedJARException {
		PackageMOJO mojo = new PackageMOJO();
		mojo.process(null);
	}

	@Test(expected = IllegalStateException.class)
	public void testNoFileOrArtifact() throws IllegalStateException, IOException, SignedJARException {
		PackageMOJO mojo = new PackageMOJO();
		ArtifactDefinition def = new ArtifactDefinition();
		mojo.process(def);
	}

	@Test(expected = IllegalStateException.class)
	public void testNoManifest() throws IllegalStateException, IOException, SignedJARException {
		PackageMOJO mojo = new PackageMOJO();
		ArtifactDefinition def = new ArtifactDefinition();
		def.setJarFile(new File("tmp"));
		mojo.process(def);
	}

	@Test
	public void testSimpleJar() throws Throwable {
		PackageMOJO mojo = new PackageMOJO();
		mojo.setOutputDirectory(TMP.toFile());

		ArtifactDefinition def = new ArtifactDefinition();
		def.setJarFile(getResource("maven-plugin-api-3.5.0.jar"));
		def.setManifestFile(getResource("maven-plugin-api.mf"));
		def.setPublishArtifact(false);
		def.setMode("merge");
		mojo.process(def);

		try (JarFile jf = new JarFile(TMP.resolve("maven-plugin-api-3.5.0.jar").toFile())) {
			Manifest man = jf.getManifest();
			Attributes attr = man.getMainAttributes();
			assertEquals("Overwrite Entry", attr.getValue("Built-By"));
			assertEquals("3.5.0", attr.getValue("Bundle-Version"));
			assertEquals("Plexus Archiver", attr.getValue("Archiver-Version"));
		} finally {
			Files.deleteIfExists(TMP.resolve("maven-plugin-api-3.5.0.jar"));
		}
	}

	@Test
	public void testSimpleJarOverwrite() throws Throwable {
		PackageMOJO mojo = new PackageMOJO();
		mojo.setOutputDirectory(TMP.toFile());

		ArtifactDefinition def = new ArtifactDefinition();
		def.setJarFile(getResource("maven-plugin-api-3.5.0.jar"));
		def.setManifestFile(getResource("maven-plugin-api.mf"));
		def.setPublishArtifact(false);
		def.setMode("overwrite");
		mojo.process(def);

		try (JarFile jf = new JarFile(TMP.resolve("maven-plugin-api-3.5.0.jar").toFile())) {
			Manifest man = jf.getManifest();
			Attributes attr = man.getMainAttributes();
			assertEquals("Overwrite Entry", attr.getValue("Built-By"));
			assertEquals("3.5.0", attr.getValue("Bundle-Version"));
			assertNull(attr.getValue("Archiver-Version"));
		} finally {
			Files.deleteIfExists(TMP.resolve("maven-plugin-api-3.5.0.jar"));
		}
	}

	@Test
	public void testSimpleArtifact() throws Throwable {
		PackageMOJO mojo = new PackageMOJO();
		mojo.setOutputDirectory(TMP.toFile());
		mojo.setLocalRepository(MVN_REPO.toFile());

		ArtifactDefinition def = new ArtifactDefinition();
		def.setArtifact("org.apache.maven:maven-plugin-api:3.5.0");
		def.setManifestFile(getResource("maven-plugin-api.mf"));
		def.setPublishArtifact(false);
		def.setMode("merge");
		mojo.process(def);

		try (JarFile jf = new JarFile(TMP.resolve("maven-plugin-api-3.5.0.jar").toFile())) {
			Manifest man = jf.getManifest();
			Attributes attr = man.getMainAttributes();
			assertEquals("Overwrite Entry", attr.getValue("Built-By"));
			assertEquals("3.5.0", attr.getValue("Bundle-Version"));
			assertEquals("Plexus Archiver", attr.getValue("Archiver-Version"));
		} finally {
			Files.deleteIfExists(TMP.resolve("maven-plugin-api-3.5.0.jar"));
		}
	}

	@Test
	public void testSimpleArtifactOverwrite() throws Throwable {
		PackageMOJO mojo = new PackageMOJO();
		mojo.setOutputDirectory(TMP.toFile());
		mojo.setLocalRepository(MVN_REPO.toFile());

		ArtifactDefinition def = new ArtifactDefinition();
		def.setArtifact("org.apache.maven:maven-plugin-api:3.5.0");
		def.setManifestFile(getResource("maven-plugin-api.mf"));
		def.setPublishArtifact(false);
		def.setMode("overwrite");
		mojo.process(def);

		try (JarFile jf = new JarFile(TMP.resolve("maven-plugin-api-3.5.0.jar").toFile())) {
			Manifest man = jf.getManifest();
			Attributes attr = man.getMainAttributes();
			assertEquals("Overwrite Entry", attr.getValue("Built-By"));
			assertEquals("3.5.0", attr.getValue("Bundle-Version"));
			assertNull(attr.getValue("Archiver-Version"));
		} finally {
			Files.deleteIfExists(TMP.resolve("maven-plugin-api-3.5.0.jar"));
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testEmptyArtifact() throws Throwable {
		PackageMOJO mojo = new PackageMOJO();
		mojo.setOutputDirectory(TMP.toFile());
		mojo.setLocalRepository(MVN_REPO.toFile());

		ArtifactDefinition def = new ArtifactDefinition();
		def.setArtifact("");
		def.setManifestFile(getResource("maven-plugin-api.mf"));
		def.setPublishArtifact(false);
		def.setMode("merge");
		mojo.process(def);
	}
	
	@Test(expected = IllegalStateException.class)
	public void testMalformedArtifact() throws Throwable {
		PackageMOJO mojo = new PackageMOJO();
		mojo.setOutputDirectory(TMP.toFile());
		mojo.setLocalRepository(MVN_REPO.toFile());

		ArtifactDefinition def = new ArtifactDefinition();
		def.setArtifact("a");
		def.setManifestFile(getResource("maven-plugin-api.mf"));
		def.setPublishArtifact(false);
		def.setMode("merge");
		mojo.process(def);
	}
	
	@Test(expected = IllegalStateException.class)
	public void testMalformedArtifact2() throws Throwable {
		PackageMOJO mojo = new PackageMOJO();
		mojo.setOutputDirectory(TMP.toFile());
		mojo.setLocalRepository(MVN_REPO.toFile());

		ArtifactDefinition def = new ArtifactDefinition();
		def.setArtifact("a:a");
		def.setManifestFile(getResource("maven-plugin-api.mf"));
		def.setPublishArtifact(false);
		def.setMode("merge");
		mojo.process(def);
	}

	@Test(expected = IllegalStateException.class)
	public void testMalformedArtifact3() throws Throwable {
		PackageMOJO mojo = new PackageMOJO();
		mojo.setOutputDirectory(TMP.toFile());
		mojo.setLocalRepository(MVN_REPO.toFile());

		ArtifactDefinition def = new ArtifactDefinition();
		def.setArtifact("a:a:A:a");
		def.setManifestFile(getResource("maven-plugin-api.mf"));
		def.setPublishArtifact(false);
		def.setMode("merge");
		mojo.process(def);
	}
	
	@Test(expected = IllegalStateException.class)
	public void testMalformedArtifact4() throws Throwable {
		PackageMOJO mojo = new PackageMOJO();
		mojo.setOutputDirectory(TMP.toFile());
		mojo.setLocalRepository(MVN_REPO.toFile());

		ArtifactDefinition def = new ArtifactDefinition();
		def.setArtifact("::");
		def.setManifestFile(getResource("maven-plugin-api.mf"));
		def.setPublishArtifact(false);
		def.setMode("merge");
		mojo.process(def);
	}
	
	@Test(expected = SignedJARException.class)
	public void testSignedArtifact() throws Throwable {
		PackageMOJO mojo = new PackageMOJO();
		mojo.setOutputDirectory(TMP.toFile());
		mojo.setLocalRepository(MVN_REPO.toFile());

		ArtifactDefinition def = new ArtifactDefinition();
		def.setArtifact("org.bouncycastle:bcpkix-jdk15on:1.60");
		def.setManifestFile(getResource("maven-plugin-api.mf"));
		def.setPublishArtifact(false);
		def.setMode("merge");
		mojo.process(def);
	}
	
	private File getResource(String resource) throws Throwable {
		return Paths.get(TestPackageMojo.class.getClassLoader().getResource(resource).toURI()).toFile();
	}

}
