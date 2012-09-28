package org.apache.maven.plugins.release;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.codehaus.plexus.util.StringUtils;

/**
 * Base class with shared configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractReleaseMojo
    extends AbstractMojo
{
    /**
     */
    @Parameter( defaultValue = "${basedir}", readonly = true, required = true )
    private File basedir;

    /**
     */
    @Component
    private Settings settings;

    /**
     */
    @Component
    protected MavenProject project;

    /**
     */
    @Component
    protected ReleaseManager releaseManager;

    /**
     * Additional arguments to pass to the Maven executions, separated by spaces.
     */
    @Parameter( alias = "prepareVerifyArgs", property = "arguments" )
    private String arguments;

    /**
     * The file name of the POM to execute any goals against.
     */
    @Parameter( property = "pomFileName" )
    private String pomFileName;

    /**
     */
    @Parameter( defaultValue = "${reactorProjects}", readonly = true, required = true )
    private List<MavenProject> reactorProjects;

    /**
     * The {@code M2_HOME} parameter to use for forked Maven invocations.
     *
     * @since 2.0-beta-8
     */
    @Parameter( defaultValue = "${maven.home}" )
    protected File mavenHome;

    /**
     * The {@code JAVA_HOME} parameter to use for forked Maven invocations.
     *
     * @since 2.0-beta-8
     */
    @Parameter( defaultValue = "${java.home}" )
    private File javaHome;

    /**
     * The command-line local repository directory in use for this build (if specified).
     *
     * @since 2.0-beta-8
     */
    @Parameter ( defaultValue = "${maven.repo.local}" )
    private File localRepoDirectory;

    /**
     * Role hint of the {@link org.apache.maven.shared.release.exec.MavenExecutor} implementation to use.
     *
     * @since 2.0-beta-8
     */
    @Parameter( defaultValue = "invoker", property = "mavenExecutorId" )
    private String mavenExecutorId;

    /**
     * @since 2.0
     */
    @Component
    protected MavenSession session;

    /**
     * Gets the enviroment settings configured for this release.
     *
     * @return The release environment, never <code>null</code>.
     */
    protected ReleaseEnvironment getReleaseEnvironment()
    {
        return new DefaultReleaseEnvironment().setSettings( settings )
                                              .setJavaHome( javaHome )
                                              .setMavenHome( mavenHome )
                                              .setLocalRepositoryDirectory( localRepoDirectory )
                                              .setMavenExecutorId( mavenExecutorId );
    }

    /**
     * Creates the release descriptor from the various goal parameters.
     *
     * @return The release descriptor, never <code>null</code>.
     */
    protected ReleaseDescriptor createReleaseDescriptor()
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();

        descriptor.setInteractive( settings.isInteractiveMode() );

        descriptor.setWorkingDirectory( basedir.getAbsolutePath() );

        descriptor.setPomFileName( pomFileName );

        @SuppressWarnings("unchecked")
        List<Profile> profiles = project.getActiveProfiles();

        String arguments = this.arguments;
        if ( profiles != null && !profiles.isEmpty() )
        {
            if ( !StringUtils.isEmpty( arguments ) )
            {
                arguments += " -P ";
            }
            else
            {
                arguments = "-P ";
            }

            for ( Iterator<Profile> it = profiles.iterator(); it.hasNext(); )
            {
                Profile profile = it.next();

                arguments += profile.getId();
                if ( it.hasNext() )
                {
                    arguments += ",";
                }
            }

            String additionalProfiles = getAdditionalProfiles();
            if ( additionalProfiles != null )
            {
                if ( !profiles.isEmpty() )
                {
                    arguments += ",";
                }
                arguments += additionalProfiles;
            }
        }
        descriptor.setAdditionalArguments( arguments );

        return descriptor;
    }

    /**
     * Gets the comma separated list of additional profiles for the release build.
     *
     * @return additional profiles to enable during release
     */
    protected String getAdditionalProfiles()
    {
        return null;
    }

    /**
     * Sets the component used to perform release actions.
     *
     * @param releaseManager The release manager implementation to use, must not be <code>null</code>.
     */
    void setReleaseManager( ReleaseManager releaseManager )
    {
        this.releaseManager = releaseManager;
    }

    /**
     * Gets the effective settings for this build.
     *
     * @return The effective settings for this build, never <code>null</code>.
     */
    Settings getSettings()
    {
        return settings;
    }
    
    protected final File getBasedir()
    {
        return basedir;
    }

    /**
     * Sets the base directory of the build.
     *
     * @param basedir The build's base directory, must not be <code>null</code>.
     */
    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    /**
     * Gets the list of projects in the build reactor.
     *
     * @return The list of reactor project, never <code>null</code>.
     */
    public List<MavenProject> getReactorProjects()
    {
        return reactorProjects;
    }

    /**
     * Add additional arguments.
     *
     * @param argument The argument to add, must not be <code>null</code>.
     */
    protected void addArgument( String argument )
    {
        if ( arguments != null )
        {
            arguments += " " + argument;
        }
        else
        {
            arguments = argument;
        }
    }

    /**
     * This method takes some of the release configuration picked up from the command line system properties and copies
     * it into the release config object.
     *
     * @param config The release configuration to merge the system properties into, must not be <code>null</code>.
     * @param sysPropertiesConfig The configuration from the system properties to merge in, must not be
     *            <code>null</code>.
     */
    protected void mergeCommandLineConfig( ReleaseDescriptor config, ReleaseDescriptor sysPropertiesConfig )
    {
        // If the user specifies versions, these should override the existing versions
        if ( sysPropertiesConfig.getReleaseVersions() != null )
        {
            config.getReleaseVersions().putAll( sysPropertiesConfig.getReleaseVersions() );
        }
        if ( sysPropertiesConfig.getDevelopmentVersions() != null )
        {
            config.getDevelopmentVersions().putAll( sysPropertiesConfig.getDevelopmentVersions() );
        }
    }
}
