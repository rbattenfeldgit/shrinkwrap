/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.shrinkwrap.impl.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Assignable;
import org.jboss.shrinkwrap.api.Configuration;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.StreamExporter;
import org.jboss.shrinkwrap.api.formatter.Formatter;
import org.jboss.shrinkwrap.api.formatter.Formatters;
import org.jboss.shrinkwrap.impl.base.asset.ArchiveAsset;
import org.jboss.shrinkwrap.impl.base.path.BasicPath;
import org.jboss.shrinkwrap.spi.Configurable;

/**
 * ArchiveBase
 * 
 * Base implementation of {@link Archive}.  Contains
 * support for operations (typically overloaded) that are 
 * not specific to any particular storage implementation, 
 * and may be delegated to other forms.
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @author <a href="mailto:baileyje@gmail.com">John Bailey</a>
 * @version $Revision: $
 */
public abstract class ArchiveBase<T extends Archive<T>> implements Archive<T>, Configurable
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(ArchiveBase.class.getName());

   //-------------------------------------------------------------------------------------||
   // Instance Members -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Name of the archive
    */
   private final String name;

   /**
    * Configuration for this archive 
    */
   private final Configuration configuration;

   //-------------------------------------------------------------------------------------||
   // Constructor ------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Constructor
    * 
    * Creates a new Archive with the specified name
    * 
    * @param name Name of the archive
    * @param configuration The configuration for this archive
    * @throws IllegalArgumentException If the name was not specified
    */
   protected ArchiveBase(final String name, final Configuration configuration) throws IllegalArgumentException
   {
      // Precondition checks
      Validate.notNullOrEmpty(name, "name must be specified");
      Validate.notNull(configuration, "configuration must be specified");

      // Set
      this.name = name;
      this.configuration = configuration;
   }

   //-------------------------------------------------------------------------------------||
   // Required Implementations -----------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#add(java.lang.String, org.jboss.shrinkwrap.api.asset.Asset)
    */
   @Override
   public T add(final Asset asset, final String target) throws IllegalArgumentException
   {
      // Precondition checks
      Validate.notNullOrEmpty(target, "target must be specified");
      Validate.notNull(asset, "asset must be specified");

      // Make a Path from the target
      final ArchivePath path = new BasicPath(target);

      // Delegate
      return this.add(asset, path);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#add(org.jboss.shrinkwrap.api.asset.Asset, java.lang.String, java.lang.String)
    */
   @Override
   public T add(final Asset asset, final String target, final String name) throws IllegalArgumentException
   {
      Validate.notNull(target, "target must be specified");
      final ArchivePath path = ArchivePaths.create(target);
      return this.add(asset, path, name);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#add(org.jboss.shrinkwrap.api.ArchivePath, java.lang.String, org.jboss.shrinkwrap.api.asset.Asset)
    */
   @Override
   public T add(final Asset asset, final ArchivePath path, final String name)
   {
      // Precondition checks
      Validate.notNull(path, "No path was specified");
      Validate.notNullOrEmpty(name, "No target name name was specified");
      Validate.notNull(asset, "No asset was was specified");

      // Make a relative path
      final ArchivePath resolvedPath = new BasicPath(path, name);

      // Delegate
      return this.add(asset, resolvedPath);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#get(java.lang.String)
    */
   @Override
   public Node get(final String path) throws IllegalArgumentException
   {
      // Precondition checks
      Validate.notNullOrEmpty(path, "No path was specified");

      // Make a Path
      final ArchivePath realPath = new BasicPath(path);

      // Delegate
      return get(realPath);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#add(org.jboss.shrinkwrap.api.Archive, org.jboss.shrinkwrap.api.ArchivePath, java.lang.Class)
    */
   @Override
   public T add(final Archive<?> archive, final ArchivePath path, Class<? extends StreamExporter> exporter)
   {
      // Precondition checks
      Validate.notNull(path, "No path was specified");
      Validate.notNull(archive, "No archive was specified");
      Validate.notNull(exporter, "No exporter was specified");

      // Make a Path
      final String archiveName = archive.getName();
      final ArchivePath contentPath = new BasicPath(path, archiveName);

      // Create ArchiveAsset 
      final ArchiveAsset archiveAsset = new ArchiveAsset(archive, exporter);

      // Delegate
      return add(archiveAsset, contentPath);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#addDirectory(java.lang.String)
    */
   @Override
   public T addDirectory(final String path) throws IllegalArgumentException
   {
      // Precondition check
      Validate.notNullOrEmpty(path, "path must be specified");

      // Delegate and return
      return this.addDirectory(ArchivePaths.create(path));
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#addDirectories(org.jboss.shrinkwrap.api.ArchivePath[])
    */
   @Override
   public T addDirectories(final ArchivePath... paths) throws IllegalArgumentException
   {
      // Precondition check
      Validate.notNull(paths, "paths must be specified");

      // Add
      for (final ArchivePath path : paths)
      {
         this.addDirectory(path);
      }

      // Return
      return covariantReturn();
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#addDirectories(java.lang.String[])
    */
   @Override
   public T addDirectories(final String... paths) throws IllegalArgumentException
   {
      // Precondition check
      Validate.notNull(paths, "paths must be specified");

      // Represent as array of Paths
      final Collection<ArchivePath> pathsCollection = new ArrayList<ArchivePath>(paths.length);
      for (final String path : paths)
      {
         pathsCollection.add(ArchivePaths.create(path));
      }

      // Delegate and return
      return this.addDirectories(pathsCollection.toArray(new ArchivePath[]
      {}));
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#getName()
    */
   public final String getName()
   {
      return name;
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#merge(org.jboss.shrinkwrap.api.Archive)
    */
   @Override
   public T merge(final Archive<?> source) throws IllegalArgumentException
   {
      return merge(source, new BasicPath());
   }

   /** 
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#merge(org.jboss.shrinkwrap.api.Archive, org.jboss.shrinkwrap.api.Filter)
    */
   @Override
   public T merge(Archive<?> source, Filter<ArchivePath> filter) throws IllegalArgumentException
   {
      return merge(source, new BasicPath(), filter);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#merge(org.jboss.shrinkwrap.api.ArchivePath, org.jboss.shrinkwrap.api.Archive)
    */
   @Override
   public T merge(final Archive<?> source, final ArchivePath path) throws IllegalArgumentException
   {
      Validate.notNull(source, "No source archive was specified");
      Validate.notNull(path, "No path was specified");

      return merge(source, path, Filters.includeAll());
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#merge(org.jboss.shrinkwrap.api.Archive, java.lang.String, org.jboss.shrinkwrap.api.Filter)
    */
   @Override
   public T merge(final Archive<?> source, final String path, final Filter<ArchivePath> filter)
         throws IllegalArgumentException
   {
      Validate.notNull(path, "path must be specified");
      return this.merge(source, ArchivePaths.create(path), filter);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#merge(org.jboss.shrinkwrap.api.Archive, java.lang.String)
    */
   @Override
   public T merge(final Archive<?> source, final String path) throws IllegalArgumentException
   {
      Validate.notNull(path, "path must be specified");
      return this.merge(source, ArchivePaths.create(path));
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#merge(org.jboss.shrinkwrap.api.Archive, org.jboss.shrinkwrap.api.Path, org.jboss.shrinkwrap.api.Filter)
    */
   @Override
   public T merge(Archive<?> source, ArchivePath path, Filter<ArchivePath> filter) throws IllegalArgumentException
   {
      // Precondition checks
      Validate.notNull(source, "No source archive was specified");
      Validate.notNull(path, "No path was specified");
      Validate.notNull(filter, "No filter was specified");

      // Get existing contents from source archive
      final Map<ArchivePath, Node> sourceContent = source.getContent();
      Validate.notNull(sourceContent, "Source archive content can not be null.");

      // Add each asset from the source archive
      for (final Entry<ArchivePath, Node> contentEntry : sourceContent.entrySet())
      {
         final Node node = contentEntry.getValue();
         ArchivePath nodePath = new BasicPath(path, contentEntry.getKey());
         if (!filter.include(nodePath))
         {
            continue;
         }
         // Delegate
         if (node.getAsset() == null)
         {
            addDirectory(nodePath);
         }
         else
         {
            add(node.getAsset(), nodePath);
         }
      }
      return covariantReturn();
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Assignable#as(java.lang.Class)
    */
   @Override
   public <TYPE extends Assignable> TYPE as(final Class<TYPE> clazz)
   {
      Validate.notNull(clazz, "Class must be specified");

      return this.configuration.getExtensionLoader().load(clazz, this);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#toString()
    */
   @Override
   public String toString()
   {
      return this.toString(Formatters.SIMPLE);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#toString(boolean)
    */
   @Override
   public String toString(final boolean verbose)
   {
      return verbose ? this.toString(Formatters.VERBOSE) : this.toString();
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#toString(org.jboss.shrinkwrap.api.formatter.Formatter)
    */
   @Override
   public String toString(final Formatter formatter) throws IllegalArgumentException
   {
      // Precondition check
      if (formatter == null)
      {
         throw new IllegalArgumentException("Formatter must be specified");
      }

      // Delegate
      return formatter.format(this);
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#hashCode()
    */
   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((getContent() == null) ? 0 : getContent().hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.api.Archive#equals(Object)
    */
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (!(obj instanceof ArchiveBase))
      {
         return false;
      }

      ArchiveBase<?> other = (ArchiveBase<?>) obj;

      if (getContent() == null)
      {
         if (other.getContent() != null)
            return false;
      }
      else if (!getContent().equals(other.getContent()))
         return false;
      if (name == null)
      {
         if (other.name != null)
            return false;
      }
      else if (!name.equals(other.name))
         return false;
      return true;
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.spi.Configurable#getConfiguration()
    */
   @Override
   public Configuration getConfiguration()
   {
      return configuration;
   }

   //-------------------------------------------------------------------------------------||
   // Contracts --------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Returns the actual typed class for this instance, used in safe casting 
    * for covariant return types
    * 
    * @return
    */
   protected abstract Class<T> getActualClass();

   //-------------------------------------------------------------------------------------||
   // Internal Helper Methods ------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Provides typesafe covariant return of this instance
    */
   protected final T covariantReturn()
   {
      try
      {
         return this.getActualClass().cast(this);
      }
      catch (final ClassCastException cce)
      {
         log.log(Level.SEVERE,
               "The class specified by getActualClass is not a valid assignment target for this instance;"
                     + " developer error");
         throw cce;
      }
   }
}
