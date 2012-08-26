/*
 * Copyright 2012 Alexey Hanin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alexeyhanin.maven.yajaxb.plugin;

import com.sun.tools.jxc.SchemaGenerator;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates schema files from source.
 *
 * @goal schemagen
 * @phase process-resources
 */
public class SchemagenMojo extends AbstractMojo {

    /**
     * Maven project.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Location of the file.
     *
     * @parameter default-value="${project.build.directory}/generates-resources/schemagen"
     * @required
     */
    private File outputDirectory;


    /**
     * Inclusion pattern for files to generate schema from.
     *
     * @parameter expression="${schemagen.includes}"
     * @required
     */
    private Set<String> includes = new HashSet<String>();

    /**
     * Exclusion pattern for files to generate schema from.
     *
     * @parameter expression="${schemagen.excludes}"
     * @required
     */
    private Set<String> excludes = new HashSet<String>();

    /**
     * Episode file to be generated if one is needed.
     *
     * @parameter expression="${schemagen.episode}"
     */
    private File episode;

    public MavenProject getProject() {
        return project;
    }

    public void execute() throws MojoExecutionException {
        if ("pom".equals(project.getPackaging())) {
            return;
        }

        if (includes == null) {
            throw new MojoExecutionException("No includes defined");
        }

        final Set<String> sources = new LinkedHashSet<String>();
        final List<String> sourceRoots = project.getCompileSourceRoots();
        for (String sourceRoot : sourceRoots) {
            try {
                sources.addAll(FileUtils.getFileNames(new File(sourceRoot), StringUtils.join(includes.iterator(), ", "),
                        StringUtils.join(excludes.iterator(), ", "), true));
            } catch (IOException ex) {
                throw new MojoExecutionException("Error scanning source root: \'" + sourceRoot + "\' " + "", ex);
            }
        }

        if (getLog().isInfoEnabled()) {
            getLog().info(String.format("Included %d source files: %s", sources.size(), StringUtils.join(
                    sources.iterator(), " ")));
        }

        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new MojoExecutionException("Unable to create output directory: " + outputDirectory.getAbsolutePath());
        }

        final List<String> classpathElements;
        try {
            classpathElements = project.getCompileClasspathElements();
        } catch (DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException("Failed to retrieve classpath", ex);
        }

        if (episode != null) {
            if (!episode.getParentFile().exists()) {
                episode.getParentFile().mkdirs();
            }
        }

        final List<String> args = new ArrayList<String>(sources.size() + 10);
        args.add("-d");
        args.add(outputDirectory.getAbsolutePath());
        if (!classpathElements.isEmpty()) {
            args.add("-classpath");
            args.add(StringUtils.join(classpathElements.iterator(), File.pathSeparator));
        }
        if (episode != null) {
            args.add("-episode");
            args.add(episode.getAbsolutePath());
        }
        args.addAll(sources);
        try {
            SchemaGenerator.run(args.toArray(new String[args.size()]));
        } catch (Exception ex) {
            throw new MojoExecutionException("Error running schema generator", ex);
        }
    }

}
