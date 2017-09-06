package com.sitewhere.microservice.spi;

import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor;

/**
 * Microservice that serves a global funcition in a SiteWhere instance.
 * 
 * @author Derek
 */
public interface IGlobalMicroservice extends IMicroservice {

    /**
     * Called to start microservice after initialization.
     * 
     * @param monitor
     * @throws SiteWhereException
     */
    public void microserviceStart(ILifecycleProgressMonitor monitor) throws SiteWhereException;
}