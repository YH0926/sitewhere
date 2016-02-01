/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.device.presence;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.sitewhere.SiteWhere;
import com.sitewhere.rest.model.device.communication.DecodedDeviceRequest;
import com.sitewhere.rest.model.device.event.request.DeviceStateChangeCreateRequest;
import com.sitewhere.rest.model.search.DateRangeSearchCriteria;
import com.sitewhere.rest.model.search.SearchCriteria;
import com.sitewhere.server.lifecycle.TenantLifecycleComponent;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.device.IDeviceAssignment;
import com.sitewhere.spi.device.IDeviceManagement;
import com.sitewhere.spi.device.ISite;
import com.sitewhere.spi.device.communication.IDecodedDeviceRequest;
import com.sitewhere.spi.device.communication.IInboundProcessingStrategy;
import com.sitewhere.spi.device.event.request.IDeviceStateChangeCreateRequest;
import com.sitewhere.spi.device.event.state.PresenceState;
import com.sitewhere.spi.device.event.state.StateChangeCategory;
import com.sitewhere.spi.device.event.state.StateChangeType;
import com.sitewhere.spi.device.presence.IDevicePresenceManager;
import com.sitewhere.spi.device.presence.IPresenceNotificationStrategy;
import com.sitewhere.spi.search.ISearchResults;
import com.sitewhere.spi.server.lifecycle.LifecycleComponentType;

/**
 * Monitors assignment state to detect device presence information.
 * 
 * @author Derek
 */
public class DevicePresenceManager extends TenantLifecycleComponent implements IDevicePresenceManager {

	/** Static logger instance */
	private static Logger LOGGER = Logger.getLogger(DevicePresenceManager.class);

	/** Default presence check interval in seconds */
	private static final int DEFAULT_PRESENCE_CHECK_INTERVAL_SECS = 60;

	/** Default presence missing interval (1 day) */
	private static final int DEFAULT_PRESENCE_MISSING_INTERVAL_SECS = 120;

	/** Presence check interval in seconds */
	private int presenceCheckIntervalSecs = DEFAULT_PRESENCE_CHECK_INTERVAL_SECS;

	/** Presence missing interval in seconds */
	private int presenceMissingIntervalSecs = DEFAULT_PRESENCE_MISSING_INTERVAL_SECS;

	/** Chooses how presence state is stored and how often notifications are sent */
	private IPresenceNotificationStrategy presenceNotificationStrategy =
			new PresenceNotificationStrategies.SendOnceNotificationStrategy();

	/** Executor service for threading */
	private ExecutorService executor;

	/** Device management implementation */
	private IDeviceManagement devices;

	/** Inbound processing strategy for tenant */
	private IInboundProcessingStrategy inbound;

	public DevicePresenceManager() {
		super(LifecycleComponentType.DevicePresenceManager);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.spi.server.lifecycle.ILifecycleComponent#start()
	 */
	@Override
	public void start() throws SiteWhereException {
		this.devices = SiteWhere.getServer().getDeviceManagement(getTenant());
		this.inbound = SiteWhere.getServer().getEventProcessing(getTenant()).getInboundProcessingStrategy();

		// Launch a presence checker thread per site.
		List<ISite> sites = devices.listSites(new SearchCriteria(1, 0)).getResults();
		this.executor = Executors.newFixedThreadPool(sites.size());
		for (ISite site : sites) {
			executor.execute(new PresenceChecker(site));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.spi.server.lifecycle.ILifecycleComponent#stop()
	 */
	@Override
	public void stop() throws SiteWhereException {
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.spi.server.lifecycle.ILifecycleComponent#getLogger()
	 */
	@Override
	public Logger getLogger() {
		return LOGGER;
	}

	/**
	 * Thread that checks for device presence.
	 * 
	 * @author Derek
	 */
	private class PresenceChecker implements Runnable {

		/** Site whose assignments are checked */
		private ISite site;

		public PresenceChecker(ISite site) {
			this.site = site;
		}

		@Override
		public void run() {
			while (true) {

				try {
					// Calculate time window for presence calculation.
					Date endDate =
							new Date(System.currentTimeMillis() - (60 * getPresenceMissingIntervalSecs()));
					DateRangeSearchCriteria criteria = new DateRangeSearchCriteria(1, 0, null, endDate);
					ISearchResults<IDeviceAssignment> missing =
							devices.getDeviceAssignmentsWithLastInteraction(site.getToken(), criteria);
					LOGGER.debug("Presence manager for '" + site.getName() + "' creating "
							+ missing.getNumResults() + " events for non-present devices.");
					for (IDeviceAssignment assignment : missing.getResults()) {
						DeviceStateChangeCreateRequest create =
								new DeviceStateChangeCreateRequest(StateChangeCategory.Presence,
										StateChangeType.Presence_Updated, PresenceState.PRESENT.name(),
										PresenceState.NOT_PRESENT.name());
						create.setUpdateState(true);

						// Only send an event if the strategy permits it.
						if (getPresenceNotificationStrategy().shouldGenerateEvent(assignment, create)) {
							IDecodedDeviceRequest<IDeviceStateChangeCreateRequest> decoded =
									new DecodedDeviceRequest<IDeviceStateChangeCreateRequest>(
											assignment.getDeviceHardwareId(), null, create);
							inbound.processDeviceStateChange(decoded);
						}
					}
				} catch (SiteWhereException e) {
					LOGGER.error("Error processing presence query.", e);
				}

				try {
					Thread.sleep(getPresenceCheckIntervalSecs() * 1000);
				} catch (InterruptedException e) {
					LOGGER.info("Presence check thread shut down.", e);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.spi.device.presence.IDevicePresenceManager#
	 * getPresenceNotificationStrategy()
	 */
	public IPresenceNotificationStrategy getPresenceNotificationStrategy() {
		return presenceNotificationStrategy;
	}

	public void setPresenceNotificationStrategy(IPresenceNotificationStrategy presenceNotificationStrategy) {
		this.presenceNotificationStrategy = presenceNotificationStrategy;
	}

	public int getPresenceCheckIntervalSecs() {
		return presenceCheckIntervalSecs;
	}

	public void setPresenceCheckIntervalSecs(int presenceCheckIntervalSecs) {
		this.presenceCheckIntervalSecs = presenceCheckIntervalSecs;
	}

	public int getPresenceMissingIntervalSecs() {
		return presenceMissingIntervalSecs;
	}

	public void setPresenceMissingIntervalSecs(int presenceMissingIntervalSecs) {
		this.presenceMissingIntervalSecs = presenceMissingIntervalSecs;
	}
}