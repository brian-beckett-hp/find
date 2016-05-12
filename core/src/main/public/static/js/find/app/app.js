/*
 * Copyright 2014-2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

define([
    'jquery',
    'backbone',
    'underscore',
    'store',
    'find/app/util/test-browser',
    'find/app/philips',
    'find/app/model/indexes-collection',
    'find/app/model/saved-searches/saved-query-collection',
    './model-registry',
    'find/app/navigation',
    'find/app/configuration',
    'find/app/pages',
    'find/app/util/logout',
    'find/app/vent',
    'find/app/router',
    'text!find/templates/app/app.html'
], function($, Backbone, _, store, testBrowser, philips, IndexesCollection, SavedQueryCollection, ModelRegistry, Navigation, configuration, Pages, logout, vent, router, template) {

    return Backbone.View.extend({
        el: '.page',
        template: _.template(template),

        // Can be overridden
        defaultPage: null,
        Navigation: Navigation,

        // Abstract
        getPageData: null,

        events: {
            'click .navigation-logout': function() {
                logout('../logout');
            }
        },

        initialize: function() {
            $.ajaxSetup({cache: false});

            $.when(testBrowser(), philips()).done(function() {
                var modelRegistry = new ModelRegistry(this.getModelData());
                var pageData = this.getPageData();

                this.pages = new Pages({
                    defaultPage: this.defaultPage,
                    modelRegistry: modelRegistry,
                    pageData: pageData,
                    router: router
                });

                this.navigation = new this.Navigation({
                    pageData: pageData,
                    router: router
                });

                this.render();

                var matchedRoute = Backbone.history.start();

                if(store.session('lastQuery')) {
                    vent.navigate('find/search')
                } else if (!matchedRoute) {
                    vent.navigate('find/search/splash');
                }
            }.bind(this));
        },

        render: function() {
            this.$el.html(this.template({
                username: configuration().username
            }));

            this.pages.render();

            this.$('.content').append(this.pages.el);

            this.navigation.render();

            this.$('.header').prepend(this.navigation.el);
        },

        // Can be overridden
        getModelData: function() {
            return {
                indexesCollection: {
                    Constructor: IndexesCollection
                },
                savedQueryCollection: {
                    Constructor: SavedQueryCollection,
                    fetchOptions: {remove: false}
                }
            };
        }
    });

});
