/*
 * Copyright 2016 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.core.savedsearches.query;

import com.hp.autonomy.frontend.find.core.test.AbstractFindIT;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.isA;
import static org.junit.Assert.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractSavedQueryServiceIT extends AbstractFindIT {
    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private SavedQueryService savedQueryService;

    @Test
    public void createFetchUpdateDelete() {
        final String title = "Any old saved search";
        final String queryText = "orange";

        final Set<String> relatedConcepts = new HashSet<>();
        relatedConcepts.add("manhattan");
        relatedConcepts.add("mid-town");

        final SavedQuery savedQuery = new SavedQuery.Builder()
                .setTitle(title)
                .setQueryText(queryText)
                .build();

        final SavedQuery createdEntity = savedQueryService.create(savedQuery);

        assertThat(createdEntity.getQueryText(), is(queryText));
        assertThat(createdEntity.getTitle(), is(title));
        assertThat(createdEntity.getId(), isA(Long.class));

        // Mimic how the update method is likely to be called - with a new entity without a user
        final SavedQuery updateEntity = new SavedQuery.Builder()
                .setId(createdEntity.getId())
                .setTitle(title)
                .setQueryText(queryText)
                .setRelatedConcepts(relatedConcepts)
                .build();

        final SavedQuery updatedEntity = savedQueryService.update(updateEntity);

        assertThat(updatedEntity.getQueryText(), is(queryText));
        assertThat(updatedEntity.getId(), is(createdEntity.getId()));
        assertThat(updatedEntity.getTitle(), is(title));

        final Collection<String> updatedConcepts = updatedEntity.getRelatedConcepts();
        assertThat(updatedConcepts, containsInAnyOrder("manhattan", "mid-town"));

        final Set<SavedQuery> fetchedEntities = savedQueryService.getAll();
        assertThat(fetchedEntities, hasSize(1));

        final SavedQuery fetchedEntity = fetchedEntities.iterator().next();
        assertThat(fetchedEntity.getTitle(), is(title));
        assertThat(fetchedEntity.getRelatedConcepts(), containsInAnyOrder("manhattan", "mid-town"));

        savedQueryService.deleteById(updatedEntity.getId());

        assertThat(savedQueryService.getAll(), is(empty()));
    }

    @Test
    @Transactional
    public void getAllReturnsNothing() throws Exception {
        assertThat(savedQueryService.getAll(), is(empty()));
    }

    @Test
    public void checkUserAuditDataInserted() {
        final SavedQuery savedQuery = new SavedQuery.Builder()
                .setTitle("title")
                .setQueryText("*")
                .build();

        savedQueryService.create(savedQuery);

        assertNotNull(savedQuery.getId());
        assertThat(savedQuery.getUser().getUserId(), isA(Long.class));
        assertNotNull(savedQuery.getUser().getUserId());
    }

    @Test
    public void checkTimeAuditDataInsertedUpdated() {
        final SavedQuery savedQuery = savedQueryService.create(new SavedQuery.Builder()
                .setTitle("title")
                .setQueryText("*")
                .build());

        assertNotNull(savedQuery.getId());
        assertThat(savedQuery.getDateCreated(), isA(DateTime.class));
        assertThat(savedQuery.getDateModified(), isA(DateTime.class));
        assertTrue(savedQuery.getDateCreated().isEqual(savedQuery.getDateModified().toInstant()));

        // Safe to assume completed in a day
        // TODO: mock out the datetime service used by spring auditing to check this properly
        assertTrue(savedQuery.getDateCreated().plusHours(1).isAfterNow());

        savedQuery.setQueryText("*");

        final SavedQuery updatedQuery = savedQueryService.update(new SavedQuery.Builder()
                .setId(savedQuery.getId())
                .setTitle("new title")
                .build());

        assertThat(updatedQuery.getDateCreated(), isA(DateTime.class));
        assertThat(updatedQuery.getDateModified(), isA(DateTime.class));
        assertTrue(updatedQuery.getDateModified().isAfter(savedQuery.getDateCreated().toInstant()));
    }

    @Test
    public void checkUserNotDuplicated() {
        final SavedQuery savedQuery1 = new SavedQuery.Builder()
                .setTitle("title1")
                .setQueryText("*")
                .build();

        final SavedQuery savedQuery2 = new SavedQuery.Builder()
                .setTitle("title2")
                .setQueryText("*")
                .build();

        savedQueryService.create(savedQuery1);
        savedQueryService.create(savedQuery2);

        assertNotNull(savedQuery1.getUser().getUserId());
        assertNotNull(savedQuery2.getUser().getUserId());
        assertEquals(savedQuery1.getUser().getUserId(), savedQuery2.getUser().getUserId());
    }
}