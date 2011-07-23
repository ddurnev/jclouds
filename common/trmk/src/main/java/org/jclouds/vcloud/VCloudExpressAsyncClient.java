/**
 *
 * Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */
package org.jclouds.vcloud;

import static org.jclouds.vcloud.VCloudMediaType.CATALOG_XML;
import static org.jclouds.vcloud.VCloudMediaType.NETWORK_XML;
import static org.jclouds.vcloud.VCloudMediaType.TASK_XML;
import static org.jclouds.vcloud.VCloudMediaType.VAPPTEMPLATE_XML;
import static org.jclouds.vcloud.VCloudMediaType.VAPP_XML;

import java.net.URI;
import java.util.Map;

import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jclouds.predicates.validators.DnsNameValidator;
import org.jclouds.rest.annotations.EndpointParam;
import org.jclouds.rest.annotations.ExceptionParser;
import org.jclouds.rest.annotations.MapBinder;
import org.jclouds.rest.annotations.ParamValidators;
import org.jclouds.rest.annotations.PayloadParam;
import org.jclouds.rest.annotations.RequestFilters;
import org.jclouds.rest.annotations.ResponseParser;
import org.jclouds.rest.annotations.XMLResponseParser;
import org.jclouds.rest.functions.ReturnNullOnNotFoundOr404;
import org.jclouds.vcloud.binders.BindCloneVCloudExpressVAppParamsToXmlPayload;
import org.jclouds.vcloud.binders.BindInstantiateVCloudExpressVAppTemplateParamsToXmlPayload;
import org.jclouds.vcloud.domain.Catalog;
import org.jclouds.vcloud.domain.ReferenceType;
import org.jclouds.vcloud.domain.Task;
import org.jclouds.vcloud.domain.VCloudExpressVApp;
import org.jclouds.vcloud.domain.VCloudExpressVAppTemplate;
import org.jclouds.vcloud.domain.network.OrgNetwork;
import org.jclouds.vcloud.endpoints.Org;
import org.jclouds.vcloud.filters.SetVCloudTokenCookie;
import org.jclouds.vcloud.functions.OrgNameAndCatalogNameToEndpoint;
import org.jclouds.vcloud.functions.OrgNameCatalogNameVAppTemplateNameToEndpoint;
import org.jclouds.vcloud.functions.OrgNameVDCNameResourceEntityNameToEndpoint;
import org.jclouds.vcloud.functions.ParseTaskFromLocationHeader;
import org.jclouds.vcloud.options.CloneVAppOptions;
import org.jclouds.vcloud.options.InstantiateVAppTemplateOptions;
import org.jclouds.vcloud.xml.OrgNetworkFromVCloudExpressNetworkHandler;
import org.jclouds.vcloud.xml.TaskHandler;
import org.jclouds.vcloud.xml.VCloudExpressCatalogHandler;
import org.jclouds.vcloud.xml.VCloudExpressVAppHandler;
import org.jclouds.vcloud.xml.VCloudExpressVAppTemplateHandler;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Provides;

/**
 * Provides access to VCloud resources via their REST API.
 * <p/>
 * 
 * @see <a href="https://community.vcloudexpress.terremark.com/en-us/discussion_forums/f/60.aspx" />
 * @author Adrian Cole
 */
@RequestFilters(SetVCloudTokenCookie.class)
public interface VCloudExpressAsyncClient extends CommonVCloudAsyncClient {
   /**
    * 
    * @return a listing of all orgs that the current user has access to.
    */
   @Provides
   @Org
   Map<String, ReferenceType> listOrgs();
   
   /**
    * @see CommonVCloudClient#getCatalog
    */
   @Override
   @GET
   @XMLResponseParser(VCloudExpressCatalogHandler.class)
   @ExceptionParser(ReturnNullOnNotFoundOr404.class)
   @Consumes(CATALOG_XML)
   ListenableFuture<? extends Catalog> getCatalog(@EndpointParam URI catalogId);

   /**
    * @see CommonVCloudClient#findCatalogInOrgNamed
    */
   @GET
   @XMLResponseParser(VCloudExpressCatalogHandler.class)
   @ExceptionParser(ReturnNullOnNotFoundOr404.class)
   @Consumes(CATALOG_XML)
   ListenableFuture<? extends Catalog> findCatalogInOrgNamed(
            @Nullable @EndpointParam(parser = OrgNameAndCatalogNameToEndpoint.class) String orgName,
            @Nullable @EndpointParam(parser = OrgNameAndCatalogNameToEndpoint.class) String catalogName);

   /**
    * @see VCloudClient#getVAppTemplate
    */
   @GET
   @Consumes(VAPPTEMPLATE_XML)
   @XMLResponseParser(VCloudExpressVAppTemplateHandler.class)
   @ExceptionParser(ReturnNullOnNotFoundOr404.class)
   ListenableFuture<? extends VCloudExpressVAppTemplate> getVAppTemplate(@EndpointParam URI vAppTemplate);

   /**
    * @see VCloudClient#findVAppTemplateInOrgCatalogNamed
    */
   @GET
   @Consumes(VAPPTEMPLATE_XML)
   @XMLResponseParser(VCloudExpressVAppTemplateHandler.class)
   @ExceptionParser(ReturnNullOnNotFoundOr404.class)
   ListenableFuture<? extends VCloudExpressVAppTemplate> findVAppTemplateInOrgCatalogNamed(
            @Nullable @EndpointParam(parser = OrgNameCatalogNameVAppTemplateNameToEndpoint.class) String orgName,
            @Nullable @EndpointParam(parser = OrgNameCatalogNameVAppTemplateNameToEndpoint.class) String catalogName,
            @EndpointParam(parser = OrgNameCatalogNameVAppTemplateNameToEndpoint.class) String itemName);

   /**
    * @see VCloudClient#findNetworkInOrgVDCNamed
    */
   @Override
   @GET
   @Consumes(NETWORK_XML)
   @XMLResponseParser(OrgNetworkFromVCloudExpressNetworkHandler.class)
   @ExceptionParser(ReturnNullOnNotFoundOr404.class)
   ListenableFuture<? extends OrgNetwork> findNetworkInOrgVDCNamed(
            @Nullable @EndpointParam(parser = OrgNameVDCNameResourceEntityNameToEndpoint.class) String orgName,
            @Nullable @EndpointParam(parser = OrgNameVDCNameResourceEntityNameToEndpoint.class) String catalogName,
            @EndpointParam(parser = OrgNameVDCNameResourceEntityNameToEndpoint.class) String networkName);

   /**
    * @see VCloudClient#getNetwork
    */
   @Override
   @GET
   @Consumes(NETWORK_XML)
   @XMLResponseParser(OrgNetworkFromVCloudExpressNetworkHandler.class)
   @ExceptionParser(ReturnNullOnNotFoundOr404.class)
   ListenableFuture<? extends OrgNetwork> getNetwork(@EndpointParam URI network);

   /**
    * @see VCloudExpressClient#instantiateVAppTemplateInVDC
    */
   @POST
   @Path("/action/instantiateVAppTemplate")
   @Produces("application/vnd.vmware.vcloud.instantiateVAppTemplateParams+xml")
   @Consumes(VAPP_XML)
   @XMLResponseParser(VCloudExpressVAppHandler.class)
   @MapBinder(BindInstantiateVCloudExpressVAppTemplateParamsToXmlPayload.class)
   ListenableFuture<? extends VCloudExpressVApp> instantiateVAppTemplateInVDC(@EndpointParam URI vdc,
            @PayloadParam("template") URI template,
            @PayloadParam("name") @ParamValidators(DnsNameValidator.class) String appName,
            InstantiateVAppTemplateOptions... options);

   /**
    * @see VCloudExpressClient#cloneVAppInVDC
    */
   @POST
   @Path("/action/cloneVApp")
   @Produces("application/vnd.vmware.vcloud.cloneVAppParams+xml")
   @Consumes(TASK_XML)
   @XMLResponseParser(TaskHandler.class)
   @MapBinder(BindCloneVCloudExpressVAppParamsToXmlPayload.class)
   ListenableFuture<? extends Task> cloneVAppInVDC(@EndpointParam URI vdc, @PayloadParam("vApp") URI toClone,
            @PayloadParam("newName") @ParamValidators(DnsNameValidator.class) String newName,
            CloneVAppOptions... options);

   /**
    * @see VCloudClient#findVAppInOrgVDCNamed
    */
   @GET
   @Consumes(VAPP_XML)
   @XMLResponseParser(VCloudExpressVAppHandler.class)
   @ExceptionParser(ReturnNullOnNotFoundOr404.class)
   ListenableFuture<? extends VCloudExpressVApp> findVAppInOrgVDCNamed(
            @Nullable @EndpointParam(parser = OrgNameVDCNameResourceEntityNameToEndpoint.class) String orgName,
            @Nullable @EndpointParam(parser = OrgNameVDCNameResourceEntityNameToEndpoint.class) String catalogName,
            @EndpointParam(parser = OrgNameVDCNameResourceEntityNameToEndpoint.class) String vAppName);

   /**
    * @see VCloudClient#getVApp
    */
   @GET
   @Consumes(VAPP_XML)
   @XMLResponseParser(VCloudExpressVAppHandler.class)
   @ExceptionParser(ReturnNullOnNotFoundOr404.class)
   ListenableFuture<? extends VCloudExpressVApp> getVApp(@EndpointParam URI vApp);

   /**
    * @see VCloudExpressClient#deployVApp
    */
   @POST
   @Consumes(TASK_XML)
   @Path("/action/deploy")
   @XMLResponseParser(TaskHandler.class)
   ListenableFuture<? extends Task> deployVApp(@EndpointParam URI vAppId);

   /**
    * @see VCloudExpressClient#undeployVApp
    */
   @POST
   @Consumes(TASK_XML)
   @Path("/action/undeploy")
   @XMLResponseParser(TaskHandler.class)
   ListenableFuture<? extends Task> undeployVApp(@EndpointParam URI vAppId);

   /**
    * @see VCloudExpressClient#powerOnVApp
    */
   @POST
   @Consumes(TASK_XML)
   @Path("/power/action/powerOn")
   @XMLResponseParser(TaskHandler.class)
   ListenableFuture<? extends Task> powerOnVApp(@EndpointParam URI vAppId);

   /**
    * @see VCloudExpressClient#powerOffVApp
    */
   @POST
   @Consumes(TASK_XML)
   @Path("/power/action/powerOff")
   @XMLResponseParser(TaskHandler.class)
   ListenableFuture<? extends Task> powerOffVApp(@EndpointParam URI vAppId);

   /**
    * @see VCloudExpressClient#shutdownVApp
    */
   @POST
   @Path("/power/action/shutdown")
   ListenableFuture<Void> shutdownVApp(@EndpointParam URI vAppId);

   /**
    * @see VCloudExpressClient#resetVApp
    */
   @POST
   @Consumes(TASK_XML)
   @Path("/power/action/reset")
   @XMLResponseParser(TaskHandler.class)
   ListenableFuture<? extends Task> resetVApp(@EndpointParam URI vAppId);

   /**
    * @see VCloudExpressClient#suspendVApp
    */
   @POST
   @Consumes(TASK_XML)
   @Path("/power/action/suspend")
   @XMLResponseParser(TaskHandler.class)
   ListenableFuture<? extends Task> suspendVApp(@EndpointParam URI vAppId);

   /**
    * @see VCloudExpressClient#deleteVApp
    */
   @DELETE
   @ResponseParser(ParseTaskFromLocationHeader.class)
   @ExceptionParser(ReturnNullOnNotFoundOr404.class)
   ListenableFuture<Task> deleteVApp(@EndpointParam URI vAppId);
}