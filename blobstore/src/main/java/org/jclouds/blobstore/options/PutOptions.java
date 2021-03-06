/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.blobstore.options;

/**
 * Contains options supported in the put blob operation. <h2>
 * Usage</h2> The recommended way to instantiate a PutOptions object is to statically import
 * PutOptions.* and invoke a static creation method followed by an instance mutator (if needed):
 * <p/>
 * <code>
 * import static org.jclouds.blobstore.options.PutOptions.Builder.*
 * eTag = blobStore.putBlob("container", blob, multipart());
 * <code>
 * 
 * @author Adrian Cole
 */
public class PutOptions implements Cloneable {

   public static final ImmutablePutOptions NONE = new ImmutablePutOptions(new PutOptions());

   private boolean multipart;

   public PutOptions() {
   }

   PutOptions(boolean multipart) {
      this.multipart = multipart;
   }

   public static class ImmutablePutOptions extends PutOptions {
      private final PutOptions delegate;

      public ImmutablePutOptions(PutOptions delegate) {
         this.delegate = delegate;
      }

      @Override
      public boolean isMultipart() {
         return delegate.isMultipart();
      }

      @Override
      public PutOptions multipart() {
         throw new UnsupportedOperationException();
      }

      @Override
      public PutOptions clone() {
         return delegate.clone();
      }

      @Override
      public String toString() {
         return delegate.toString();
      }

   }

   public boolean isMultipart() {
      return multipart;
   }

   /**
    * split large blobs into pieces, if supported by the provider
    */
   public PutOptions multipart() {
      this.multipart = true;
      return this;
   }

   public static class Builder {

      /**
       * @see PutOptions#multipart()
       */
      public static PutOptions multipart() {
         PutOptions options = new PutOptions();
         return options.multipart();
      }

   }

   @Override
   public PutOptions clone() {
      return new PutOptions(multipart);
   }

   @Override
   public String toString() {
      return "[multipart=" + multipart + "]";
   }
}
