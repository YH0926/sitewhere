/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.web.rest.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sitewhere.grpc.client.event.BlockingDeviceEventManagement;
import com.sitewhere.instance.spi.microservice.IInstanceManagementMicroservice;
import com.sitewhere.microservice.api.asset.IAssetManagement;
import com.sitewhere.microservice.api.device.AreaMarshalHelper;
import com.sitewhere.microservice.api.device.DeviceAssignmentMarshalHelper;
import com.sitewhere.microservice.api.device.IDeviceManagement;
import com.sitewhere.microservice.api.device.asset.DeviceAlertWithAsset;
import com.sitewhere.microservice.api.device.asset.DeviceCommandInvocationWithAsset;
import com.sitewhere.microservice.api.device.asset.DeviceCommandResponseWithAsset;
import com.sitewhere.microservice.api.device.asset.DeviceLocationWithAsset;
import com.sitewhere.microservice.api.device.asset.DeviceMeasurementsWithAsset;
import com.sitewhere.microservice.api.device.asset.DeviceStateChangeWithAsset;
import com.sitewhere.microservice.api.event.IDeviceEventManagement;
import com.sitewhere.microservice.api.label.ILabelGeneration;
import com.sitewhere.rest.model.area.request.AreaCreateRequest;
import com.sitewhere.rest.model.device.DeviceAssignment;
import com.sitewhere.rest.model.search.SearchResults;
import com.sitewhere.rest.model.search.area.AreaSearchCriteria;
import com.sitewhere.rest.model.search.device.DeviceAssignmentSearchCriteria;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.SiteWhereSystemException;
import com.sitewhere.spi.area.IArea;
import com.sitewhere.spi.device.DeviceAssignmentStatus;
import com.sitewhere.spi.device.IDeviceAssignment;
import com.sitewhere.spi.device.event.DeviceEventIndex;
import com.sitewhere.spi.device.event.IDeviceAlert;
import com.sitewhere.spi.device.event.IDeviceCommandInvocation;
import com.sitewhere.spi.device.event.IDeviceCommandResponse;
import com.sitewhere.spi.device.event.IDeviceLocation;
import com.sitewhere.spi.device.event.IDeviceMeasurement;
import com.sitewhere.spi.device.event.IDeviceStateChange;
import com.sitewhere.spi.error.ErrorCode;
import com.sitewhere.spi.error.ErrorLevel;
import com.sitewhere.spi.label.ILabel;
import com.sitewhere.spi.search.IDateRangeSearchCriteria;
import com.sitewhere.spi.search.ISearchResults;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

/**
 * Controller for area operations.
 */
@Path("/api/areas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "areas")
public class Areas {

    /** Static logger instance */
    @SuppressWarnings("unused")
    private static Log LOGGER = LogFactory.getLog(Areas.class);

    @Inject
    private IInstanceManagementMicroservice<?> microservice;

    /**
     * Create a new area.
     * 
     * @param input
     * @return
     * @throws SiteWhereException
     */
    @POST
    @ApiOperation(value = "Create new area")
    public Response createArea(@RequestBody AreaCreateRequest input) throws SiteWhereException {
	return Response.ok(getDeviceManagement().createArea(input)).build();
    }

    /**
     * Get information for a given area based on token.
     * 
     * @param areaToken
     * @param includeAreaType
     * @param includeParentArea
     * @return
     * @throws SiteWhereException
     */
    @GET
    @Path("/{areaToken}")
    @ApiOperation(value = "Get area by token")
    public Response getAreaByToken(
	    @ApiParam(value = "Token that identifies area", required = true) @PathParam("areaToken") String areaToken,
	    @ApiParam(value = "Include area type", required = false) @QueryParam("includeAreaType") @DefaultValue("false") boolean includeAreaType,
	    @ApiParam(value = "Include parent area information", required = false) @QueryParam("includeParentArea") @DefaultValue("true") boolean includeParentArea)
	    throws SiteWhereException {
	IArea existing = assertArea(areaToken);
	AreaMarshalHelper helper = new AreaMarshalHelper(getDeviceManagement(), getAssetManagement());
	helper.setIncludeAreaType(includeAreaType);
	helper.setIncludeParentArea(includeParentArea);
	return Response.ok(helper.convert(existing)).build();
    }

    /**
     * Update information for an area.
     * 
     * @param areaToken
     * @param request
     * @return
     * @throws SiteWhereException
     */
    @PUT
    @Path("/{areaToken}")
    @ApiOperation(value = "Update existing area")
    public Response updateArea(
	    @ApiParam(value = "Token that identifies area", required = true) @PathParam("areaToken") String areaToken,
	    @RequestBody AreaCreateRequest request) throws SiteWhereException {
	IArea existing = assertArea(areaToken);
	return Response.ok(getDeviceManagement().updateArea(existing.getId(), request)).build();
    }

    /**
     * Get label for area based on a specific generator.
     * 
     * @param areaToken
     * @param generatorId
     * @return
     * @throws SiteWhereException
     */
    @GET
    @Path("/{areaToken}/label/{generatorId}")
    @Produces("image/png")
    @ApiOperation(value = "Get label for area")
    public Response getAreaLabel(
	    @ApiParam(value = "Token that identifies area", required = true) @PathParam("areaToken") String areaToken,
	    @ApiParam(value = "Generator id", required = true) @PathParam("areaToken") String generatorId)
	    throws SiteWhereException {
	IArea existing = assertArea(areaToken);
	ILabel label = getLabelGeneration().getAreaLabel(generatorId, existing.getId());
	if (label == null) {
	    return Response.status(Status.NOT_FOUND).build();
	}
	return Response.ok(label.getContent()).build();
    }

    /**
     * List areas matching criteria.
     * 
     * @param rootOnly
     * @param parentAreaToken
     * @param areaTypeToken
     * @param includeAreaType
     * @param includeAssignments
     * @param includeZones
     * @param page
     * @param pageSize
     * @return
     * @throws SiteWhereException
     */
    @GET
    @ApiOperation(value = "List areas matching criteria")
    public Response listAreas(
	    @ApiParam(value = "Limit to root elements", required = false) @QueryParam("rootOnly") @DefaultValue("true") Boolean rootOnly,
	    @ApiParam(value = "Limit by parent area token", required = false) @QueryParam("parentAreaToken") String parentAreaToken,
	    @ApiParam(value = "Limit by area type token", required = false) @QueryParam("areaTypeToken") String areaTypeToken,
	    @ApiParam(value = "Include area type", required = false) @QueryParam("includeAreaType") @DefaultValue("false") boolean includeAreaType,
	    @ApiParam(value = "Include assignments", required = false) @QueryParam("includeAssignments") @DefaultValue("false") boolean includeAssignments,
	    @ApiParam(value = "Include zones", required = false) @QueryParam("includeZones") @DefaultValue("false") boolean includeZones,
	    @ApiParam(value = "Page number", required = false) @QueryParam("page") @DefaultValue("1") int page,
	    @ApiParam(value = "Page size", required = false) @QueryParam("pageSize") @DefaultValue("100") int pageSize)
	    throws SiteWhereException {
	// Build criteria.
	AreaSearchCriteria criteria = buildAreaSearchCriteria(page, pageSize, rootOnly, parentAreaToken, areaTypeToken);

	// Perform search.
	ISearchResults<? extends IArea> matches = getDeviceManagement().listAreas(criteria);
	AreaMarshalHelper helper = new AreaMarshalHelper(getDeviceManagement(), getAssetManagement());
	helper.setIncludeAreaType(includeAreaType);
	helper.setIncludeZones(includeZones);
	helper.setIncludeAssignments(includeAssignments);

	List<IArea> results = new ArrayList<IArea>();
	for (IArea area : matches.getResults()) {
	    results.add(helper.convert(area));
	}
	return Response.ok(new SearchResults<IArea>(results, matches.getNumResults())).build();
    }

    /**
     * List all areas in a hierarchical tree format.
     * 
     * @return
     * @throws SiteWhereException
     */
    @GET
    @Path("/tree")
    @ApiOperation(value = "List all areas in tree format")
    public Response getAreasTree() throws SiteWhereException {
	return Response.ok(getDeviceManagement().getAreasTree()).build();
    }

    /**
     * Build area search criteria from parameters.
     * 
     * @param page
     * @param pageSize
     * @param rootOnly
     * @param parentAreaToken
     * @param areaTypeToken
     * @return
     * @throws SiteWhereException
     */
    public static AreaSearchCriteria buildAreaSearchCriteria(int page, int pageSize, boolean rootOnly,
	    String parentAreaToken, String areaTypeToken) throws SiteWhereException {
	// Build criteria.
	AreaSearchCriteria criteria = new AreaSearchCriteria(page, pageSize);
	criteria.setRootOnly(rootOnly);
	criteria.setParentAreaToken(parentAreaToken);
	criteria.setAreaTypeToken(areaTypeToken);
	return criteria;
    }

    /**
     * Delete information for a given area based on token.
     * 
     * @param areaToken
     * @return
     * @throws SiteWhereException
     */
    @DELETE
    @Path("/{areaToken}")
    @ApiOperation(value = "Delete area by token")
    public Response deleteArea(
	    @ApiParam(value = "Token that identifies area", required = true) @PathParam("areaToken") String areaToken)
	    throws SiteWhereException {
	IArea existing = assertArea(areaToken);
	return Response.ok(getDeviceManagement().deleteArea(existing.getId())).build();
    }

    /**
     * Get device measurements for an area.
     * 
     * @param areaToken
     * @param page
     * @param pageSize
     * @param startDate
     * @param endDate
     * @param response
     * @return
     * @throws SiteWhereException
     */
    @GET
    @Path("/{areaToken}/measurements")
    @ApiOperation(value = "List measurements for an area")
    public Response listDeviceMeasurementsForArea(
	    @ApiParam(value = "Token that identifies area", required = true) @PathParam("areaToken") String areaToken,
	    @ApiParam(value = "Page number", required = false) @QueryParam("page") @DefaultValue("1") int page,
	    @ApiParam(value = "Page size", required = false) @QueryParam("pageSize") @DefaultValue("100") int pageSize,
	    @ApiParam(value = "Start date", required = false) @QueryParam("startDate") String startDate,
	    @ApiParam(value = "End date", required = false) @QueryParam("endDate") String endDate)
	    throws SiteWhereException {
	List<UUID> areas = resolveAreaIdsRecursive(areaToken, true, getDeviceManagement());
	IDateRangeSearchCriteria criteria = Assignments.createDateRangeSearchCriteria(page, pageSize, startDate,
		endDate);
	ISearchResults<IDeviceMeasurement> results = getDeviceEventManagement()
		.listDeviceMeasurementsForIndex(DeviceEventIndex.Area, areas, criteria);

	// Marshal with asset info since multiple assignments might match.
	List<IDeviceMeasurement> wrapped = new ArrayList<IDeviceMeasurement>();
	for (IDeviceMeasurement result : results.getResults()) {
	    wrapped.add(new DeviceMeasurementsWithAsset(result, getAssetManagement()));
	}
	return Response.ok(new SearchResults<IDeviceMeasurement>(wrapped, results.getNumResults())).build();
    }

    /**
     * Get device locations for an area.
     * 
     * @param areaToken
     * @param page
     * @param pageSize
     * @param startDate
     * @param endDate
     * @return
     * @throws SiteWhereException
     */
    @GET
    @Path("/{areaToken}/locations")
    @ApiOperation(value = "List locations for an area")
    public Response listDeviceLocationsForArea(
	    @ApiParam(value = "Token that identifies area", required = true) @PathParam("areaToken") String areaToken,
	    @ApiParam(value = "Page number", required = false) @QueryParam("page") @DefaultValue("1") int page,
	    @ApiParam(value = "Page size", required = false) @QueryParam("pageSize") @DefaultValue("100") int pageSize,
	    @ApiParam(value = "Start date", required = false) @QueryParam("startDate") String startDate,
	    @ApiParam(value = "End date", required = false) @QueryParam("endDate") String endDate)
	    throws SiteWhereException {
	List<UUID> areas = resolveAreaIdsRecursive(areaToken, true, getDeviceManagement());
	IDateRangeSearchCriteria criteria = Assignments.createDateRangeSearchCriteria(page, pageSize, startDate,
		endDate);
	ISearchResults<IDeviceLocation> results = getDeviceEventManagement()
		.listDeviceLocationsForIndex(DeviceEventIndex.Area, areas, criteria);

	// Marshal with asset info since multiple assignments might match.
	List<IDeviceLocation> wrapped = new ArrayList<IDeviceLocation>();
	for (IDeviceLocation result : results.getResults()) {
	    wrapped.add(new DeviceLocationWithAsset(result, getAssetManagement()));
	}
	return Response.ok(new SearchResults<IDeviceLocation>(wrapped, results.getNumResults())).build();
    }

    /**
     * Get device alerts for an area.
     * 
     * @param areaToken
     * @param page
     * @param pageSize
     * @param startDate
     * @param endDate
     * @param response
     * @return
     * @throws SiteWhereException
     */
    @GET
    @Path("/{areaToken}/alerts")
    @ApiOperation(value = "List alerts for an area")
    public Response listDeviceAlertsForArea(
	    @ApiParam(value = "Token that identifies area", required = true) @PathParam("areaToken") String areaToken,
	    @ApiParam(value = "Page number", required = false) @QueryParam("page") @DefaultValue("1") int page,
	    @ApiParam(value = "Page size", required = false) @QueryParam("pageSize") @DefaultValue("100") int pageSize,
	    @ApiParam(value = "Start date", required = false) @QueryParam("startDate") String startDate,
	    @ApiParam(value = "End date", required = false) @QueryParam("endDate") String endDate)
	    throws SiteWhereException {
	IDateRangeSearchCriteria criteria = Assignments.createDateRangeSearchCriteria(page, pageSize, startDate,
		endDate);
	List<UUID> areas = resolveAreaIdsRecursive(areaToken, true, getDeviceManagement());
	ISearchResults<IDeviceAlert> results = getDeviceEventManagement()
		.listDeviceAlertsForIndex(DeviceEventIndex.Area, areas, criteria);

	// Marshal with asset info since multiple assignments might match.
	List<IDeviceAlert> wrapped = new ArrayList<IDeviceAlert>();
	for (IDeviceAlert result : results.getResults()) {
	    wrapped.add(new DeviceAlertWithAsset(result, getAssetManagement()));
	}
	return Response.ok(new SearchResults<IDeviceAlert>(wrapped, results.getNumResults())).build();
    }

    /**
     * Get device command invocations for an area.
     * 
     * @param areaToken
     * @param page
     * @param pageSize
     * @param startDate
     * @param endDate
     * @return
     * @throws SiteWhereException
     */
    @GET
    @Path("/{areaToken}/invocations")
    @ApiOperation(value = "List command invocations for an area")
    public Response listDeviceCommandInvocationsForArea(
	    @ApiParam(value = "Token that identifies area", required = true) @PathParam("areaToken") String areaToken,
	    @ApiParam(value = "Page number", required = false) @QueryParam("page") @DefaultValue("1") int page,
	    @ApiParam(value = "Page size", required = false) @QueryParam("pageSize") @DefaultValue("100") int pageSize,
	    @ApiParam(value = "Start date", required = false) @QueryParam("startDate") String startDate,
	    @ApiParam(value = "End date", required = false) @QueryParam("endDate") String endDate)
	    throws SiteWhereException {
	List<UUID> areas = resolveAreaIdsRecursive(areaToken, true, getDeviceManagement());
	IDateRangeSearchCriteria criteria = Assignments.createDateRangeSearchCriteria(page, pageSize, startDate,
		endDate);
	ISearchResults<IDeviceCommandInvocation> results = getDeviceEventManagement()
		.listDeviceCommandInvocationsForIndex(DeviceEventIndex.Area, areas, criteria);

	// Marshal with asset info since multiple assignments might match.
	List<IDeviceCommandInvocation> wrapped = new ArrayList<IDeviceCommandInvocation>();
	for (IDeviceCommandInvocation result : results.getResults()) {
	    wrapped.add(new DeviceCommandInvocationWithAsset(result, getAssetManagement()));
	}
	return Response.ok(new SearchResults<IDeviceCommandInvocation>(wrapped, results.getNumResults())).build();
    }

    /**
     * Get device command responses for an area.
     * 
     * @param areaToken
     * @param page
     * @param pageSize
     * @param startDate
     * @param endDate
     * @param response
     * @return
     * @throws SiteWhereException
     */
    @GET
    @Path("/{areaToken}/responses")
    @ApiOperation(value = "List command responses for an area")
    public Response listDeviceCommandResponsesForArea(
	    @ApiParam(value = "Token that identifies area", required = true) @PathParam("areaToken") String areaToken,
	    @ApiParam(value = "Page number", required = false) @QueryParam("page") @DefaultValue("1") int page,
	    @ApiParam(value = "Page size", required = false) @QueryParam("pageSize") @DefaultValue("100") int pageSize,
	    @ApiParam(value = "Start date", required = false) @QueryParam("startDate") String startDate,
	    @ApiParam(value = "End date", required = false) @QueryParam("endDate") String endDate)
	    throws SiteWhereException {
	List<UUID> areas = resolveAreaIdsRecursive(areaToken, true, getDeviceManagement());
	IDateRangeSearchCriteria criteria = Assignments.createDateRangeSearchCriteria(page, pageSize, startDate,
		endDate);
	ISearchResults<IDeviceCommandResponse> results = getDeviceEventManagement()
		.listDeviceCommandResponsesForIndex(DeviceEventIndex.Area, areas, criteria);

	// Marshal with asset info since multiple assignments might match.
	List<IDeviceCommandResponse> wrapped = new ArrayList<IDeviceCommandResponse>();
	for (IDeviceCommandResponse result : results.getResults()) {
	    wrapped.add(new DeviceCommandResponseWithAsset(result, getAssetManagement()));
	}
	return Response.ok(new SearchResults<IDeviceCommandResponse>(wrapped, results.getNumResults())).build();
    }

    /**
     * Get device state changes for an area.
     * 
     * @param areaToken
     * @param page
     * @param pageSize
     * @param startDate
     * @param endDate
     * @return
     * @throws SiteWhereException
     */
    @GET
    @Path("/{areaToken}/statechanges")
    @ApiOperation(value = "List state changes associated with an area")
    public Response listDeviceStateChangesForArea(
	    @ApiParam(value = "Token that identifies area", required = true) @PathParam("areaToken") String areaToken,
	    @ApiParam(value = "Page number", required = false) @QueryParam("page") @DefaultValue("1") int page,
	    @ApiParam(value = "Page size", required = false) @QueryParam("pageSize") @DefaultValue("100") int pageSize,
	    @ApiParam(value = "Start date", required = false) @QueryParam("startDate") String startDate,
	    @ApiParam(value = "End date", required = false) @QueryParam("endDate") String endDate)
	    throws SiteWhereException {
	List<UUID> areas = resolveAreaIdsRecursive(areaToken, true, getDeviceManagement());
	IDateRangeSearchCriteria criteria = Assignments.createDateRangeSearchCriteria(page, pageSize, startDate,
		endDate);
	ISearchResults<IDeviceStateChange> results = getDeviceEventManagement()
		.listDeviceStateChangesForIndex(DeviceEventIndex.Area, areas, criteria);

	// Marshal with asset info since multiple assignments might match.
	List<IDeviceStateChange> wrapped = new ArrayList<IDeviceStateChange>();
	for (IDeviceStateChange result : results.getResults()) {
	    wrapped.add(new DeviceStateChangeWithAsset(result, getAssetManagement()));
	}
	return Response.ok(new SearchResults<IDeviceStateChange>(wrapped, results.getNumResults())).build();
    }

    /**
     * Find device assignments associated with an area.
     * 
     * @param areaToken
     * @param status
     * @param includeDevice
     * @param includeCustomer
     * @param includeArea
     * @param includeAsset
     * @param page
     * @param pageSize
     * @return
     * @throws SiteWhereException
     */
    @GET
    @Path("/{areaToken}/assignments")
    @ApiOperation(value = "List device assignments for an area")
    public Response listAssignmentsForArea(
	    @ApiParam(value = "Token that identifies area", required = true) @PathParam("areaToken") String areaToken,
	    @ApiParam(value = "Limit results to the given status", required = false) @QueryParam("status") String status,
	    @ApiParam(value = "Include device information", required = false) @QueryParam("includeDevice") @DefaultValue("false") boolean includeDevice,
	    @ApiParam(value = "Include customer information", required = false) @QueryParam("includeCustomer") @DefaultValue("false") boolean includeCustomer,
	    @ApiParam(value = "Include area information", required = false) @QueryParam("includeArea") @DefaultValue("false") boolean includeArea,
	    @ApiParam(value = "Include asset information", required = false) @QueryParam("includeAsset") @DefaultValue("false") boolean includeAsset,
	    @ApiParam(value = "Page number", required = false) @QueryParam("page") @DefaultValue("1") int page,
	    @ApiParam(value = "Page size", required = false) @QueryParam("pageSize") @DefaultValue("100") int pageSize)
	    throws SiteWhereException {
	DeviceAssignmentSearchCriteria criteria = new DeviceAssignmentSearchCriteria(page, pageSize);
	DeviceAssignmentStatus decodedStatus = (status != null) ? DeviceAssignmentStatus.valueOf(status) : null;
	if (decodedStatus != null) {
	    criteria.setAssignmentStatuses(Collections.singletonList(decodedStatus));
	}
	List<String> areas = resolveAreaTokensRecursive(areaToken, true, getDeviceManagement());
	criteria.setAreaTokens(areas);

	ISearchResults<? extends IDeviceAssignment> matches = getDeviceManagement().listDeviceAssignments(criteria);
	DeviceAssignmentMarshalHelper helper = new DeviceAssignmentMarshalHelper(getDeviceManagement());
	helper.setIncludeDevice(includeDevice);
	helper.setIncludeCustomer(includeCustomer);
	helper.setIncludeArea(includeArea);
	helper.setIncludeAsset(includeAsset);

	List<DeviceAssignment> converted = new ArrayList<DeviceAssignment>();
	for (IDeviceAssignment assignment : matches.getResults()) {
	    converted.add(helper.convert(assignment, getAssetManagement()));
	}
	return Response.ok(new SearchResults<DeviceAssignment>(converted, matches.getNumResults())).build();
    }

    /**
     * Get area associated with token or throw an exception if invalid.
     * 
     * @param token
     * @return
     * @throws SiteWhereException
     */
    protected IArea assertArea(String token) throws SiteWhereException {
	IArea area = getDeviceManagement().getAreaByToken(token);
	if (area == null) {
	    throw new SiteWhereSystemException(ErrorCode.InvalidAreaToken, ErrorLevel.ERROR);
	}
	return area;
    }

    /**
     * Resolve tokens recursively for subareas based on area token.
     * 
     * @param areaToken
     * @param recursive
     * @param deviceManagement
     * @return
     * @throws SiteWhereException
     */
    public static List<String> resolveAreaTokensRecursive(String areaToken, boolean recursive,
	    IDeviceManagement deviceManagement) throws SiteWhereException {
	List<IArea> areas = resolveAreas(areaToken, recursive, deviceManagement);
	List<String> tokens = new ArrayList<>();
	for (IArea area : areas) {
	    tokens.add(area.getToken());
	}
	return tokens;
    }

    /**
     * Resolve ids recursively for subareas based on area token.
     * 
     * @param areaToken
     * @param recursive
     * @param deviceManagement
     * @return
     * @throws SiteWhereException
     */
    public static List<UUID> resolveAreaIdsRecursive(String areaToken, boolean recursive,
	    IDeviceManagement deviceManagement) throws SiteWhereException {
	List<IArea> areas = resolveAreas(areaToken, recursive, deviceManagement);
	List<UUID> ids = new ArrayList<>();
	for (IArea area : areas) {
	    ids.add(area.getId());
	}
	return ids;
    }

    /**
     * Resolve areas including nested areas.
     * 
     * @param areaToken
     * @param recursive
     * @param deviceManagement
     * @return
     * @throws SiteWhereException
     */
    public static List<IArea> resolveAreas(String areaToken, boolean recursive, IDeviceManagement deviceManagement)
	    throws SiteWhereException {
	IArea existing = deviceManagement.getAreaByToken(areaToken);
	if (existing == null) {
	    return new ArrayList<IArea>();
	}
	Map<String, IArea> resolved = new HashMap<>();
	resolveAreasRecursively(existing, recursive, resolved, deviceManagement);
	List<IArea> response = new ArrayList<>();
	response.addAll(resolved.values());
	return response;
    }

    /**
     * Resolve areas recursively.
     * 
     * @param current
     * @param recursive
     * @param matches
     * @param deviceManagement
     * @throws SiteWhereException
     */
    protected static void resolveAreasRecursively(IArea current, boolean recursive, Map<String, IArea> matches,
	    IDeviceManagement deviceManagement) throws SiteWhereException {
	matches.put(current.getToken(), current);
	List<? extends IArea> children = deviceManagement.getAreaChildren(current.getToken());
	for (IArea child : children) {
	    resolveAreasRecursively(child, recursive, matches, deviceManagement);
	}
    }

    protected IDeviceManagement getDeviceManagement() {
	return getMicroservice().getDeviceManagement();
    }

    protected IDeviceEventManagement getDeviceEventManagement() {
	return new BlockingDeviceEventManagement(getMicroservice().getDeviceEventManagementApiChannel());
    }

    protected IAssetManagement getAssetManagement() {
	return getMicroservice().getAssetManagement();
    }

    protected ILabelGeneration getLabelGeneration() {
	return getMicroservice().getLabelGenerationApiChannel();
    }

    protected IInstanceManagementMicroservice<?> getMicroservice() {
	return microservice;
    }
}