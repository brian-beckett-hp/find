/*
 * Copyright 2014-2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

define([
    'js-whatever/js/base-page',
    'backbone',
    'find/app/configuration',
    'find/app/model/dates-filter-model',
    'parametric-refinement/selected-values-collection',
    'find/app/model/indexes-collection',
    'find/app/model/documents-collection',
    'find/app/page/search/input-view',
    'find/app/page/search/tabbed-search-view',
    'find/app/util/model-any-changed-attribute-listener',
    'find/app/util/merge-collection',
    'find/app/model/saved-searches/saved-search-model',
    'find/app/page/search/query-middle-column-header-view',
    'find/app/model/min-score-model',
    'find/app/model/query-text-model',
    'find/app/model/document-model',
    'find/app/page/search/document/document-detail-view',
    'find/app/page/search/results/query-strategy',
    'find/app/page/search/related-concepts/related-concepts-click-handlers',
    'find/app/util/database-name-resolver',
    'find/app/router',
    'find/app/vent',
    'moment',
    'store',
    'i18n!find/nls/bundle',
    'jquery',
    'underscore',
    'text!find/templates/app/page/find-search.html'
], function (BasePage, Backbone, config, DatesFilterModel, SelectedParametricValuesCollection, IndexesCollection, DocumentsCollection,
             InputView, TabbedSearchView, addChangeListener, MergeCollection, SavedSearchModel, QueryMiddleColumnHeaderView, MinScoreModel,
             QueryTextModel, DocumentModel, DocumentDetailView, queryStrategy, relatedConceptsClickHandlers, databaseNameResolver, router, vent, moment, store, i18n, $, _, template) {

    'use strict';

    var reducedClasses = 'reverse-animated-container col-md-offset-1 col-lg-offset-2 col-xs-12 col-sm-12 col-md-10 col-lg-8';
    var expandedClasses = 'animated-container col-sm-offset-0 col-md-offset-3 col-lg-offset-3 col-md-6 col-lg-6 col-xs-12 col-sm-12';
    var QUERY_TEXT_MODEL_ATTRIBUTES = ['inputText', 'relatedConcepts'];

    var html = _.template(template)({i18n: i18n});

    function selectInitialIndexes(indexesCollection) {
        var privateIndexes = indexesCollection.reject({domain: 'PUBLIC_INDEXES'});
        var selectedIndexes;

        if (privateIndexes.length > 0) {
            selectedIndexes = privateIndexes;
        } else {
            selectedIndexes = indexesCollection.models;
        }

        return _.map(selectedIndexes, function (indexModel) {
            return indexModel.pick('domain', 'name');
        });
    }

    function fetchDocument(options, callback) {
        var documentModel = new DocumentModel();

        documentModel
            .fetch({
                data: {
                    reference: options.reference,
                    database: options.database
                }
            }).done(function () {
                callback(documentModel);
            });
    }

    function attributesToJson(attributes) {
        return _.defaults({
            minDate: attributes.minDate ? attributes.minDate.unix() : null,
            maxDate: attributes.maxDate ? attributes.maxDate.unix() : null
        }, attributes);
    }

    function jsonToAttributes(json) {
        return _.defaults({
            minDate: json.minDate ? moment.unix(json.minDate) : null,
            maxDate: json.maxDate ? moment.unix(json.maxDate) : null
        }, json);
    }

    return BasePage.extend({
        className: 'search-page',
        template: _.template(template),

        // Callbacks to bind the search bar to the active tab; will be removed and added as the user changes tabs
        searchChangeCallback: null,
        queryTextCallback: null,

        // May be overridden
        QueryMiddleColumnHeaderView: QueryMiddleColumnHeaderView,
        serviceViewOptions: _.constant({}),

        // Abstract
        ServiceView: null,
        SuggestView: null,
        documentDetailOptions: null,
        suggestOptions: null,
        QueryLeftSideView: null,

        initialize: function (options) {
            this.savedQueryCollection = options.savedQueryCollection;
            this.indexesCollection = options.indexesCollection;

            this.searchTypes = this.getSearchTypes();

            this.searchCollections = _.mapObject(this.searchTypes, function (data) {
                return options[data.collection];
            });

            this.savedSearchCollection = new MergeCollection([], {
                collections: _.values(this.searchCollections)
            });

            this.selectedTabModel = new Backbone.Model({
                selectedSearchCid: null
            });

            // Model representing search bar text and related concepts
            this.searchModel = new QueryTextModel();

            // Model mapping saved search cids to query state
            this.queryStates = new Backbone.Model();

            // Map of saved search cid to ServiceView
            this.serviceViews = {};

            var lastQuery = store.session('lastQuery');

            if (lastQuery) {
                this.createNewTab(jsonToAttributes(lastQuery));
            }

            this.listenTo(this.selectedTabModel, 'change', this.selectContentView);

            this.listenTo(this.searchModel, 'change', function () {
                // Bind search model to routing
                vent.navigate(this.generateURL(), {trigger: false});

                if (this.searchModel.get('inputText')) {
                    this.expandedState();

                    // Create a tab if the user has run a search but has no open tabs
                    if (this.selectedTabModel.get('selectedSearchCid') === null) {
                        this.createNewTab({queryText: this.searchModel.get('inputText')});
                    }
                }
            });

            this.listenTo(this.savedSearchCollection, 'add', function (model) {
                if (this.selectedTabModel.get('selectedCid') === null) {
                    this.selectedTabModel.set('selectedCid', model.cid);
                }
            });

            this.listenTo(this.savedSearchCollection, 'remove', function (savedSearch) {
                var cid = savedSearch.cid;
                this.serviceViews[cid].view.remove();
                this.queryStates.unset(cid);
                delete this.serviceViews[cid];

                if (this.selectedTabModel.get('selectedSearchCid') === cid) {
                    var lastModel = this.savedQueryCollection.last();

                    if (lastModel) {
                        this.selectedTabModel.set('selectedSearchCid', lastModel.cid);
                    } else {
                        // If the user closes their last tab, run a search for *
                        this.createNewTab();
                    }
                }
            });

            this.inputView = new InputView({model: this.searchModel});

            this.tabView = new TabbedSearchView({
                savedSearchCollection: this.savedSearchCollection,
                model: this.selectedTabModel,
                queryStates: this.queryStates,
                searchTypes: this.searchTypes
            });

            this.listenTo(this.tabView, 'startNewSearch', this.createNewTab);

            this.listenTo(router, 'route:searchSplash', function () {
                this.selectedTabModel.set('selectedSearchCid', null);

                this.searchModel.set({
                    inputText: '',
                    relatedConcepts: []
                });

                this.reducedState();
            }, this);

            // Bind routing to search model
            this.listenTo(router, 'route:search', function (text) {
                this.removeDocumentDetailView();

                this.searchModel.set({
                    inputText: text || ''
                });

                if (this.isExpanded()) {
                    this.$('.service-view-container').addClass('hide');
                    this.$('.query-service-view-container').removeClass('hide');
                }
            }, this);

            this.listenTo(router, 'route:documentDetail', function () {
                this.expandedState();
                this.$('.service-view-container').addClass('hide');
                this.$('.document-detail-service-view-container').removeClass('hide');

                this.removeDocumentDetailView();

                var options = this.documentDetailOptions.apply(this, arguments);

                fetchDocument(options, function (documentModel) {
                    this.documentDetailView = new DocumentDetailView({
                        backUrl: this.generateURL(),
                        model: documentModel,
                        indexesCollection: this.indexesCollection
                    });

                    this.$('.document-detail-service-view-container').append(this.documentDetailView.$el);
                    this.documentDetailView.render();
                }.bind(this));
            }, this);

            this.listenTo(router, 'route:suggest', function () {
                this.expandedState();
                this.$('.service-view-container').addClass('hide');
                this.$('.suggest-service-view-container').removeClass('hide');

                var options = this.suggestOptions.apply(this, arguments);

                fetchDocument(options, function (documentModel) {
                    this.suggestView = new this.SuggestView({
                        backUrl: this.generateURL(),
                        documentModel: documentModel,
                        indexesCollection: this.indexesCollection
                    });

                    this.$('.suggest-service-view-container').append(this.suggestView.$el);
                    this.suggestView.render();
                }.bind(this));
            }, this);

            var savedSearchConfig = config().savedSearchConfig;
            if (savedSearchConfig.pollForUpdates) {
                this.savedSearchScheduleId = setInterval(_.bind(function () {
                    this.savedQueryCollection.fetch({remove: false}).done(_.bind(function () {
                        this.savedQueryCollection.forEach(function (savedQuery) {
                            $.ajax('../api/public/saved-query/new-results/' + savedQuery.id)
                                .success(function (newResults) {
                                    // TODO: FIND-23
                                })
                        })
                    }, this))
                }, this), savedSearchConfig.pollingInterval * 60 * 1000);
            }
        },

        render: function () {
            this.$el.html(html);

            this.inputView.setElement(this.$('.input-view-container')).render();
            this.tabView.setElement(this.$('.search-tabs-container')).render();

            if (this.selectedTabModel.get('selectedSearchCid') === null) {
                this.reducedState();
            } else {
                this.expandedState();
            }

            _.each(this.serviceViews, function (data) {
                this.$('.query-service-view-container').append(data.view.$el);
                data.view.render();
            }, this);

            this.selectContentView();
        },

        // Can be overridden
        getSearchTypes: function () {
            return {
                QUERY: {
                    autoCorrect: true,
                    collection: 'savedQueryCollection',
                    fetchStrategy: queryStrategy,
                    icon: 'hp-search',
                    isMutable: true,
                    relatedConceptsClickHandler: relatedConceptsClickHandlers.updateQuery,
                    LeftSideFooterView: this.QueryLeftSideView,
                    DocumentsCollection: DocumentsCollection,
                    MiddleColumnHeaderView: this.QueryMiddleColumnHeaderView,
                    createSearchModelAttributes: function (queryTextModel) {
                        return queryTextModel.pick(QUERY_TEXT_MODEL_ATTRIBUTES);
                    },
                    queryTextModelChange: function (options) {
                        return function () {
                            options.searchModel.set(options.queryTextModel.pick(QUERY_TEXT_MODEL_ATTRIBUTES));
                        };
                    },
                    searchModelChange: function (options) {
                        return function () {
                            options.queryTextModel.set(options.searchModel.pick(QUERY_TEXT_MODEL_ATTRIBUTES));
                        };
                    },
                    entityClickHandler: function (options) {
                        return function (text) {
                            options.queryTextModel.set('inputText', text);
                        };
                    }
                }
            };
        },

        createNewTab: function (options) {
            var attributes = _.defaults(options || {}, {
                queryText: '*',
                indexes: [],
                parametricValues: [],
                maxDate: null,
                minDate: null,
                relatedConcepts: [],
                type: SavedSearchModel.Type.QUERY,
                title: i18n['search.newSearch']
            });

            var newSearch = new SavedSearchModel(attributes);

            this.savedQueryCollection.add(newSearch);
            this.selectedTabModel.set('selectedSearchCid', newSearch.cid);
        },

        selectContentView: function () {
            var cid = this.selectedTabModel.get('selectedSearchCid');

            _.each(this.serviceViews, function (data) {
                data.view.$el.addClass('hide');
                this.stopListening(data.queryTextModel);
            }, this);

            if (this.searchChangeCallback !== null) {
                this.stopListening(this.searchModel, 'change', this.searchChangeCallback);
                this.searchChangeCallback = null;
            }

            if (this.queryTextCallback !== null) {
                this.stopListening(this.searchModel, 'change', this.queryTextCallback);
                this.queryTextCallback = null;
            }

            if (cid) {
                var viewData;
                var savedSearchModel = this.savedSearchCollection.get(cid);
                var searchType = savedSearchModel.get('type');
                var queryState;

                if (this.serviceViews[cid]) {
                    viewData = this.serviceViews[cid];
                    queryState = this.queryStates.get(cid);
                } else {
                    var queryTextModel = new QueryTextModel(savedSearchModel.toQueryTextModelAttributes());
                    var minScore = new MinScoreModel(savedSearchModel.toMinScoreModelAttributes());
                    var documentsCollection = new this.searchTypes[searchType].DocumentsCollection();

                    queryState = {
                        queryTextModel: queryTextModel,
                        minScoreModel: minScore,
                        datesFilterModel: new DatesFilterModel(savedSearchModel.toDatesFilterModelAttributes()),
                        selectedParametricValues: new SelectedParametricValuesCollection(savedSearchModel.toSelectedParametricValues())
                    };

                    var initialSelectedIndexes;
                    var savedSelectedIndexes = savedSearchModel.toSelectedIndexes();

                    if (savedSelectedIndexes.length === 0) {
                        if (this.indexesCollection.isEmpty()) {
                            initialSelectedIndexes = [];

                            this.listenToOnce(this.indexesCollection, 'sync', function () {
                                queryState.selectedIndexes.set(selectInitialIndexes(this.indexesCollection));
                            });
                        } else {
                            initialSelectedIndexes = selectInitialIndexes(this.indexesCollection);
                        }
                    } else {
                        initialSelectedIndexes = savedSelectedIndexes;
                    }

                    queryState.selectedIndexes = new IndexesCollection(initialSelectedIndexes);

                    this.queryStates.set(cid, queryState);

                    this.serviceViews[cid] = viewData = {
                        queryTextModel: queryTextModel,
                        documentsCollection: documentsCollection,
                        view: new this.ServiceView(_.extend({
                            indexesCollection: this.indexesCollection,
                            documentsCollection: documentsCollection,
                            selectedTabModel: this.selectedTabModel,
                            savedSearchCollection: this.savedSearchCollection,
                            searchCollections: this.searchCollections,
                            searchTypes: this.searchTypes,
                            queryState: queryState,
                            savedSearchModel: savedSearchModel
                        }, this.serviceViewOptions()))
                    };

                    this.$('.query-service-view-container').append(viewData.view.$el);
                    viewData.view.render();
                }

                this.searchModel.set(this.searchTypes[searchType].createSearchModelAttributes(viewData.queryTextModel));

                var changeListenerOptions = {
                    savedQueryCollection: this.savedQueryCollection,
                    selectedTabModel: this.selectedTabModel,
                    searchModel: this.searchModel,
                    queryTextModel: viewData.queryTextModel
                };

                this.queryTextCallback = addChangeListener(this, viewData.queryTextModel, QUERY_TEXT_MODEL_ATTRIBUTES, this.searchTypes[searchType].queryTextModelChange(changeListenerOptions));
                this.searchChangeCallback = addChangeListener(this, this.searchModel, QUERY_TEXT_MODEL_ATTRIBUTES, this.searchTypes[searchType].searchModelChange(changeListenerOptions));

                viewData.view.$el.removeClass('hide');

                this.updateQueryTracking(queryState);
                this.saveQueryState();
            }
        },

        saveQueryState: function() {
            store.session('lastQuery', attributesToJson(SavedSearchModel.attributesFromQueryState(this.currentQueryState)));
        },

        updateQueryTracking: function (queryState) {
            if (this.currentQueryState) {
                _.each(this.currentQueryState, function (listenedObject) {
                    this.stopListening(listenedObject, 'change update', this.saveQueryState);
                }, this)
            }

            this.listenTo(queryState.queryTextModel, 'change', this.saveQueryState);
            this.listenTo(queryState.datesFilterModel, 'change', this.saveQueryState);
            this.listenTo(queryState.selectedParametricValues, 'update', this.saveQueryState);
            this.listenTo(queryState.selectedIndexes, 'update', this.saveQueryState);

            this.currentQueryState = queryState;
        },

        generateURL: function () {
            var inputText = this.searchModel.get('inputText');

            if (this.searchModel.isEmpty()) {
                if (this.selectedTabModel.get('selectedSearchCid')) {
                    return 'find/search/query';
                } else {
                    return 'find/search/splash';
                }
            } else {
                return 'find/search/query/' + encodeURIComponent(inputText);
            }
        },

        // Run fancy animation from large central search bar to main search page
        expandedState: function () {
            this.$('.find').removeClass(reducedClasses).addClass(expandedClasses);

            this.$('.service-view-container').addClass('hide');
            this.$('.query-service-view-container').removeClass('hide');
            this.$('.app-logo').addClass('hide');
            this.$('.hp-logo-footer').addClass('hide');
            this.$('.see-all-documents').addClass('hide');

            this.removeDocumentDetailView();
            this.removeSuggestView();

            this.inputView.unFocus();
            this.$('.find-banner-container').addClass('hide');

            // TODO: somebody else needs to own this
            $('.container-fluid, .find-logo-small').removeClass('reduced');
        },

        // Set view to initial state (large central search bar)
        reducedState: function () {
            this.$('.find').removeClass(expandedClasses).addClass(reducedClasses);

            this.$('.service-view-container').addClass('hide');
            this.$('.app-logo').removeClass('hide');
            this.$('.hp-logo-footer').removeClass('hide');
            this.$('.see-all-documents').removeClass('hide');

            this.removeDocumentDetailView();
            this.removeSuggestView();

            this.inputView.focus();
            this.$('.find-banner-container').removeClass('hide');

            // TODO: somebody else needs to own this
            $('.container-fluid, .find-logo-small').addClass('reduced');
        },

        isExpanded: function () {
            return this.$('.find').hasClass(expandedClasses);
        },

        removeDocumentDetailView: function () {
            if (this.documentDetailView) {
                this.documentDetailView.remove();
                this.stopListening(this.documentDetailView);
                this.documentDetailView = null;
            }
        },

        removeSuggestView: function () {
            if (this.suggestView) {
                this.suggestView.remove();
                this.stopListening(this.suggestView);
                this.suggestView = null;
            }
        },

        remove: function () {
            clearInterval(this.savedSearchScheduleId);
            this.removeDocumentDetailView();
            Backbone.View.prototype.remove.call(this);
        }
    });
});
