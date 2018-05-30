/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.rules.microservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sitewhere.microservice.instance.InstanceSettings;
import com.sitewhere.rules.spi.microservice.IRuleProcessingMicroservice;
import com.sitewhere.spi.microservice.instance.IInstanceSettings;

/**
 * Spring bean configuration for microservice.
 * 
 * @author Derek
 */
@Configuration
public class RuleProcessingMicroserviceConfiguration {

    @Bean
    public IRuleProcessingMicroservice ruleProcessingMicroservice() {
	return new RuleProcessingMicroservice();
    }

    @Bean
    public IInstanceSettings instanceSettings() {
	return new InstanceSettings();
    }
}