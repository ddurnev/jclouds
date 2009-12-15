/**
 *
 * Copyright (C) 2009 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
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
 * ====================================================================
 */
package org.jclouds.vcloud.binders;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.jclouds.vcloud.reference.VCloudConstants.PROPERTY_VCLOUD_DEFAULTCPUCOUNT;
import static org.jclouds.vcloud.reference.VCloudConstants.PROPERTY_VCLOUD_DEFAULTMEMORY;
import static org.jclouds.vcloud.reference.VCloudConstants.PROPERTY_VCLOUD_DEFAULTNETWORK;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.http.HttpRequest;
import org.jclouds.rest.MapBinder;
import org.jclouds.rest.binders.BindToStringPayload;
import org.jclouds.rest.internal.GeneratedHttpRequest;
import org.jclouds.vcloud.options.InstantiateVAppTemplateOptions;

import com.google.common.collect.Maps;

/**
 * 
 * @author Adrian Cole
 * 
 */
@Singleton
public class BindInstantiateVAppTemplateParamsToXmlPayload implements MapBinder {

   private final String xmlTemplate;
   private final BindToStringPayload stringBinder;
   protected final Map<String, String> defaultParams;

   @Inject
   public BindInstantiateVAppTemplateParamsToXmlPayload(
            @Named("InstantiateVAppTemplateParams") String xmlTemplate,
            BindToStringPayload stringBinder,
            @Named(PROPERTY_VCLOUD_DEFAULTNETWORK) String defaultNetwork,
            @Named(PROPERTY_VCLOUD_DEFAULTCPUCOUNT) String defaultCpuCount,
            @Named(PROPERTY_VCLOUD_DEFAULTMEMORY) String defaultMemory) {
      this.xmlTemplate = xmlTemplate;
      this.stringBinder = stringBinder;
      this.defaultParams = Maps.newHashMap();
      this.defaultParams.put("network", defaultNetwork);
      this.defaultParams.put("count", defaultCpuCount);
      this.defaultParams.put("megabytes", defaultMemory);
   }

   @SuppressWarnings("unchecked")
   public void bindToRequest(HttpRequest request, Map<String, String> postParams) {
      checkArgument(checkNotNull(request, "request") instanceof GeneratedHttpRequest,
               "this binder is only valid for GeneratedHttpRequests!");
      GeneratedHttpRequest gRequest = (GeneratedHttpRequest) request;
      checkState(gRequest.getArgs() != null, "args should be initialized at this point");
      postParams = new HashMap<String, String>(postParams);
      postParams.putAll(defaultParams);
      addOptionsToMap(postParams, gRequest);

      String payload = xmlTemplate;
      for (Entry<String, String> entry : postParams.entrySet()) {
         payload = payload.replaceAll("\\{" + entry.getKey() + "\\}", entry.getValue());
      }
      stringBinder.bindToRequest(request, payload);
   }

   protected void addOptionsToMap(Map<String, String> postParams, GeneratedHttpRequest<?> gRequest) {
      for (Object arg : gRequest.getArgs()) {
         if (arg instanceof InstantiateVAppTemplateOptions) {
            InstantiateVAppTemplateOptions options = (InstantiateVAppTemplateOptions) arg;
            if (options.getCpuCount() != null) {
               postParams.put("count", options.getCpuCount());
            }
            if (options.getMegabytes() != null) {
               postParams.put("megabytes", options.getMegabytes());
            }
            if (options.getNetwork() != null) {
               postParams.put("network", options.getNetwork());
            }
         }
      }
   }

   public void bindToRequest(HttpRequest request, Object input) {
      throw new IllegalStateException("InstantiateVAppTemplateParams is needs parameters");
   }

   String ifNullDefaultTo(String value, String defaultValue) {
      return value != null ? value : checkNotNull(defaultValue, "defaultValue");
   }
}