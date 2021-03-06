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
package org.jclouds.softlayer.compute.strategy;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceAdapter;
import org.jclouds.compute.domain.Template;
import org.jclouds.domain.Credentials;
import org.jclouds.predicates.RetryablePredicate;
import org.jclouds.softlayer.SoftLayerClient;
import org.jclouds.softlayer.compute.functions.ProductItems;
import org.jclouds.softlayer.compute.options.SoftLayerTemplateOptions;
import org.jclouds.softlayer.domain.*;
import org.jclouds.softlayer.features.AccountClient;
import org.jclouds.softlayer.features.ProductPackageClient;
import org.jclouds.softlayer.reference.SoftLayerConstants;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.jclouds.softlayer.predicates.ProductItemPredicates.*;
import static org.jclouds.softlayer.predicates.ProductPackagePredicates.named;

/**
 * defines the connection between the {@link SoftLayerClient} implementation and the jclouds
 * {@link ComputeService}
 * 
 */
@Singleton
public class SoftLayerComputeServiceAdapter implements
         ComputeServiceAdapter<VirtualGuest, Set<ProductItem>, ProductItem, Datacenter> {

   public static final String SAN_DESCRIPTION_REGEX = ".*GB \\(SAN\\).*";
   private static final Float BOOT_VOLUME_CAPACITY = 100F;

   private final SoftLayerClient client;
   private final String virtualGuestPackageName;
   private final RetryablePredicate<VirtualGuest> loginDetailsTester;
   private final long guestLoginDelay;

   @Inject
   public SoftLayerComputeServiceAdapter(SoftLayerClient client,
            @Named(SoftLayerConstants.PROPERTY_SOFTLAYER_VIRTUALGUEST_PACKAGE_NAME) String virtualGuestPackageName,
            VirtualGuestHasLoginDetailsPresent virtualGuestHasLoginDetailsPresent,
            @Named(SoftLayerConstants.PROPERTY_SOFTLAYER_VIRTUALGUEST_LOGIN_DETAILS_DELAY) long guestLoginDelay)  {
      this.client = checkNotNull(client, "client");
      this.virtualGuestPackageName = checkNotNull(virtualGuestPackageName, "virtualGuestPackageName");
      checkArgument(guestLoginDelay > 500, "guestOrderDelay must be in milliseconds and greater than 500");
      this.guestLoginDelay =guestLoginDelay;

      this.loginDetailsTester = new RetryablePredicate<VirtualGuest>(virtualGuestHasLoginDetailsPresent,
               guestLoginDelay);
   }

   @Override
   public VirtualGuest createNodeWithGroupEncodedIntoNameThenStoreCredentials(String group, String name,
            Template template, Map<String, Credentials> credentialStore) {
      checkNotNull(template, "template was null");
      checkNotNull(template.getOptions(), "template options was null");
      checkArgument(template.getOptions().getClass().isAssignableFrom(SoftLayerTemplateOptions.class),
            "options class %s should have been assignable from SoftLayerTemplateOptions", template.getOptions()
            .getClass());
      
      String domainName = template.getOptions().as(SoftLayerTemplateOptions.class).getDomainName();

      VirtualGuest newGuest = VirtualGuest.builder().domain(domainName).hostname(name).build();

      ProductOrder order = ProductOrder.builder().packageId(getProductPackage().getId()).location(
               template.getLocation().getId()).quantity(1).useHourlyPricing(true).prices(getPrices(template))
               .virtualGuest(newGuest).build();

      ProductOrderReceipt productOrderReceipt = client.getVirtualGuestClient().orderVirtualGuest(order);
      VirtualGuest result = Iterables.get(productOrderReceipt.getOrderDetails().getVirtualGuests(), 0);

      boolean orderInSystem = loginDetailsTester.apply(result);

      checkState(orderInSystem, "order for guest %s doesn't have login details within %sms", result, Long.toString(guestLoginDelay));
      result = client.getVirtualGuestClient().getVirtualGuest(result.getId());

      Password pw = Iterables.get(result.getOperatingSystem().getPasswords(),0);
      Credentials credentials = new Credentials(pw.getUsername(), pw.getPassword());
      credentialStore.put("node#" + result.getId(), credentials);
      return result;
   }


   private Iterable<ProductItemPrice> getPrices(Template template) {
      Set<ProductItemPrice> result = Sets.newLinkedHashSet();

      int imageId = Integer.parseInt(template.getImage().getId());
      result.add(ProductItemPrice.builder().id(imageId).build());

      Iterable<String> hardwareIds = Splitter.on(",").split(template.getHardware().getId());
      for (String hardwareId : hardwareIds) {
         int id = Integer.parseInt(hardwareId);
         result.add(ProductItemPrice.builder().id(id).build());
      }

      result.addAll(SoftLayerConstants.DEFAULT_VIRTUAL_GUEST_PRICES);

      return result;
   }

   @Override
   public Iterable<Set<ProductItem>> listHardwareProfiles() {
      ProductPackage productPackage = getProductPackage();
      Set<ProductItem> items = productPackage.getItems();

      Iterable<ProductItem> cpuItems = Iterables.filter(items, units("PRIVATE_CORE"));
      Iterable<ProductItem> ramItems = Iterables.filter(items, categoryCode("ram"));
      Iterable<ProductItem> sanItems = Iterables.filter(items, Predicates.and(matches(SAN_DESCRIPTION_REGEX),
               categoryCode("one_time_charge")));

      Map<Float, ProductItem> cpuMap = Maps.uniqueIndex(cpuItems, ProductItems.capacity());
      Map<Float, ProductItem> ramMap = Maps.uniqueIndex(ramItems, ProductItems.capacity());
      Map<Float, ProductItem> sanMap = Maps.uniqueIndex(sanItems, ProductItems.capacity());

      final ProductItem bootVolume = sanMap.get(BOOT_VOLUME_CAPACITY);
      assert bootVolume != null : "Boot volume capacity not found:" + BOOT_VOLUME_CAPACITY + ", available:" + sanItems;

      Set<Set<ProductItem>> result = Sets.newLinkedHashSet();
      for (Map.Entry<Float, ProductItem> coresEntry : cpuMap.entrySet()) {
         Float cores = coresEntry.getKey();
         ProductItem ramItem = ramMap.get(cores);
         // Amount of RAM and number of cores must match.
         if (ramItem == null)
            continue;

         result.add(ImmutableSet.of(coresEntry.getValue(), ramItem, bootVolume));
      }

      return result;
   }

   @Override
   public Iterable<ProductItem> listImages() {
      return Iterables.filter(getProductPackage().getItems(), categoryCode("os"));
   }

   @Override
   public Iterable<VirtualGuest> listNodes() {
      return client.getVirtualGuestClient().listVirtualGuests();
   }

   @Override
   public Iterable<Datacenter> listLocations() {
      return getProductPackage().getDatacenters();
   }

   private ProductPackage getProductPackage() {
      AccountClient accountClient = client.getAccountClient();
      ProductPackageClient productPackageClient = client.getProductPackageClient();

      ProductPackage p = Iterables.find(accountClient.getActivePackages(), named(virtualGuestPackageName));
      return productPackageClient.getProductPackage(p.getId());
   }

   @Override
   public VirtualGuest getNode(String id) {
      long serverId = Long.parseLong(id);
      return client.getVirtualGuestClient().getVirtualGuest(serverId);
   }

   @Override
   public void destroyNode(String id) {
      VirtualGuest guest = getNode(id);
      if (guest == null)
         return;

      if (guest.getBillingItemId() == -1)
         return;

      client.getVirtualGuestClient().cancelService(guest.getBillingItemId());
   }

   @Override
   public void rebootNode(String id) {
      client.getVirtualGuestClient().rebootHardVirtualGuest(Long.parseLong(id));
   }

   @Override
   public void resumeNode(String id) {
      client.getVirtualGuestClient().resumeVirtualGuest(Long.parseLong(id));
   }

   @Override
   public void suspendNode(String id) {
      client.getVirtualGuestClient().pauseVirtualGuest(Long.parseLong(id));
   }

   public static class VirtualGuestHasLoginDetailsPresent implements Predicate<VirtualGuest> {
      private final SoftLayerClient client;

      @Inject
      public VirtualGuestHasLoginDetailsPresent(SoftLayerClient client) {
         this.client = checkNotNull(client, "client was null");
      }

      @Override
      public boolean apply(VirtualGuest guest) {
         checkNotNull(guest, "virtual guest was null");

         VirtualGuest newGuest = client.getVirtualGuestClient().getVirtualGuest(guest.getId());
         boolean hasBackendIp = newGuest.getPrimaryBackendIpAddress() != null;
         boolean hasPrimaryIp = newGuest.getPrimaryIpAddress() != null;
         boolean hasPasswords = newGuest.getOperatingSystem()!=null && newGuest.getOperatingSystem().getPasswords().size() > 0;

         return hasBackendIp && hasPrimaryIp && hasPasswords;
      }
   }
}