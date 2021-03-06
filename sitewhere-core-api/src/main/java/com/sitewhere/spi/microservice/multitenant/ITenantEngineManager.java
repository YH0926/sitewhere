/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.spi.microservice.multitenant;

import java.util.UUID;

import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.microservice.configuration.ITenantPathInfo;
import com.sitewhere.spi.server.lifecycle.ILifecycleComponent;

/**
 * Manages tenant engines for a multitenant microservice.
 */
public interface ITenantEngineManager<T extends IMicroserviceTenantEngine> extends ILifecycleComponent {

    /**
     * Get tenant engine corresponding to the given id.
     * 
     * @param tenantId
     * @return
     * @throws SiteWhereException
     */
    public T getTenantEngineByTenantId(UUID tenantId) throws SiteWhereException;

    /**
     * Make sure the given tenant engine exists and is started.
     * 
     * @param tenantId
     * @return
     * @throws SiteWhereException
     */
    public T assureTenantEngineAvailable(UUID tenantId) throws TenantEngineNotAvailableException;

    /**
     * Get configuration for the given tenant.
     * 
     * @param tenantId
     * @return
     * @throws SiteWhereException
     */
    public byte[] getTenantConfiguration(UUID tenantId) throws SiteWhereException;

    /**
     * Update configuration for the given tenant.
     * 
     * @param tenantId
     * @param content
     * @throws SiteWhereException
     */
    public void updateTenantConfiguration(UUID tenantId, byte[] content) throws SiteWhereException;

    /**
     * Handle configuration added.
     * 
     * @param pathInfo
     * @param data
     * @throws SiteWhereException
     */
    public void onConfigurationAdded(ITenantPathInfo pathInfo, byte[] data) throws SiteWhereException;

    /**
     * Handle configuration updated.
     * 
     * @param pathInfo
     * @param data
     * @throws SiteWhereException
     */
    public void onConfigurationUpdated(ITenantPathInfo pathInfo, byte[] data) throws SiteWhereException;

    /**
     * Handle configuration deleted.
     * 
     * @param pathInfo
     * @throws SiteWhereException
     */
    public void onConfigurationDeleted(ITenantPathInfo pathInfo) throws SiteWhereException;

    /**
     * Shuts down and restarts the given tenant engine.
     * 
     * @param tenantId
     * @throws SiteWhereException
     */
    public void restartTenantEngine(UUID tenantId) throws SiteWhereException;

    /**
     * Restart all tenant engines.
     * 
     * @throws SiteWhereException
     */
    public void restartAllTenantEngines() throws SiteWhereException;

    /**
     * Shuts down and removes a tenant engine.
     * 
     * @param tenantId
     * @throws SiteWhereException
     */
    public void removeTenantEngine(UUID tenantId) throws SiteWhereException;

    /**
     * Remove all tenant engines for the microservice.
     * 
     * @throws SiteWhereException
     */
    public void removeAllTenantEngines() throws SiteWhereException;
}
