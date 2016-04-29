define([
    'backbone',
    'find/idol/app/model/comparison/comparison-documents-collection',
    'find/idol/app/page/search/results/idol-results-view',
    'find/idol/app/page/search/results/comparison-lists',
    'find/app/page/search/results/state-token-strategy',
    'find/app/util/results-view-container',
    'find/app/util/results-view-selection',
    'text!find/idol/templates/comparison/comparison-view.html',
    'text!find/idol/templates/comparison/comparison-list-container.html',
    'find/app/util/search-data-util',
    'i18n!find/nls/bundle',
    'i18n!find/idol/nls/comparisons'
], function(Backbone, ComparisonDocumentsCollection, ResultsView, ResultsLists,  stateTokenStrategy, ResultsViewContainer, ResultsViewSelection,
            template, comparisonListContainer, searchDataUtil, i18n, comparisonsI18n) {

    return Backbone.View.extend({
        className: 'service-view-container',
        template: _.template(template),

        events: {
            'click .comparison-view-back-button': function() {
                this.escapeCallback();
            }
        },

        initialize: function(options) {
            this.searchModels = options.searchModels;
            this.escapeCallback = options.escapeCallback;

            this.resultsLists = new ResultsLists({
                searchModels: options.searchModels,
                escapeCallback: options.escapeCallback,
                model: this.model
            });

            var resultsViews = [{
                content: this.resultsLists,
                id: 'list',
                uniqueId: _.uniqueId('results-view-item-'),
                selector: {
                    displayNameKey: 'list',
                    icon: 'hp-list'
                }
            }];

            var resultsViewSelectionModel = new Backbone.Model({
                // ID of the currently selected tab
                selectedTab: resultsViews[0].id
            });

            this.resultsViewSelection = new ResultsViewSelection({
                views: resultsViews,
                model: resultsViewSelectionModel
            });

            this.resultsViewContainer = new ResultsViewContainer({
                views: resultsViews,
                model: resultsViewSelectionModel
            });
        },

        render: function() {
            this.$el.html(this.template({i18n: i18n}));

            this.resultsViewSelection.setElement(this.$('.results-view-selection')).render();
            this.resultsViewContainer.setElement(this.$('.results-view-container')).render();
        }
    });

});