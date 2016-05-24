/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.hod.parametricfields;

import com.hp.autonomy.frontend.configuration.ConfigService;
import com.hp.autonomy.frontend.find.core.parametricfields.ParametricValues;
import com.hp.autonomy.frontend.find.core.parametricfields.ParametricValuesController;
import com.hp.autonomy.frontend.find.core.search.QueryRestrictionsBuilder;
import com.hp.autonomy.frontend.find.hod.configuration.HodFindConfig;
import com.hp.autonomy.hod.client.api.resource.ResourceIdentifier;
import com.hp.autonomy.hod.client.api.textindex.query.fields.FieldType;
import com.hp.autonomy.hod.client.error.HodErrorException;
import com.hp.autonomy.searchcomponents.core.fields.FieldsService;
import com.hp.autonomy.searchcomponents.core.parametricvalues.ParametricValuesService;
import com.hp.autonomy.searchcomponents.core.search.QueryRestrictions;
import com.hp.autonomy.searchcomponents.hod.fields.HodFieldsRequest;
import com.hp.autonomy.searchcomponents.hod.parametricvalues.HodParametricRequest;
import com.hp.autonomy.types.requests.idol.actions.tags.QueryTagInfo;
import com.hp.autonomy.types.requests.idol.actions.tags.TagResponse;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping(ParametricValuesController.PARAMETRIC_VALUES_PATH)
public class HodParametricValuesController extends ParametricValuesController<HodParametricRequest, ResourceIdentifier, HodErrorException> {
    private final FieldsService<HodFieldsRequest, HodErrorException> fieldsService;
    private final ConfigService<HodFindConfig> findConfigService;

    @Autowired
    public HodParametricValuesController(final ParametricValuesService<HodParametricRequest, ResourceIdentifier, HodErrorException> parametricValuesService, final QueryRestrictionsBuilder<ResourceIdentifier> queryRestrictionsBuilder, FieldsService<HodFieldsRequest, HodErrorException> fieldsService, final ConfigService<HodFindConfig> configService) {
        super(parametricValuesService, queryRestrictionsBuilder);
        this.fieldsService = fieldsService;
        this.findConfigService = configService;
    }

    @Override
    public ParametricValues getParametricValuesInternal(
            final String queryText,
            final String fieldText,
            final List<ResourceIdentifier> databases,
            final DateTime minDate,
            final DateTime maxDate,
            final Integer minScore,
            final List<String> stateTokens
    ) throws HodErrorException {
        // Fetch parametric field names from HOD
        final TagResponse tagResponse = fieldsService.getFields(new HodFieldsRequest.Builder().setDatabases(databases).build(), Arrays.asList(FieldType.parametric.name(), FieldType.numeric.name()));
        final List<String> fieldNames = tagResponse.getParametricTypeFields();

        Set<String> fieldBlacklist = findConfigService.getConfig().getFieldBlacklist();

        if(fieldBlacklist != null) {
            fieldNames.removeAll(fieldBlacklist);
        }

        // Get parametric values for query
        final QueryRestrictions<ResourceIdentifier> queryRestrictions = queryRestrictionsBuilder.build(queryText, fieldText, databases, minDate, maxDate, minScore, stateTokens, Collections.<String>emptyList());

        final HodParametricRequest parametricRequest = new HodParametricRequest.Builder()
                .setFieldNames(fieldNames)
                .setQueryRestrictions(queryRestrictions)
                .setModified(false)
                .build();

        final Set<QueryTagInfo> parametricValues = parametricValuesService.getAllParametricValues(parametricRequest);

        // Sort values into numeric and text fields
        final Set<QueryTagInfo> pureParametricValues = new LinkedHashSet<>();
        final Set<QueryTagInfo> numericParametricValues = new LinkedHashSet<>();

        final Set<String> numericFields = new HashSet<>(tagResponse.getNumericTypeFields());

        for (final QueryTagInfo queryTagInfo : parametricValues) {
            if (numericFields.contains(queryTagInfo.getName())) {
                numericParametricValues.add(queryTagInfo);
            } else {
                pureParametricValues.add(queryTagInfo);
            }
        }

        return new ParametricValues(pureParametricValues, numericParametricValues);
    }
}
