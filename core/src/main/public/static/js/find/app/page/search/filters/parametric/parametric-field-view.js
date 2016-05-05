define([
    'backbone',
    'underscore',
    'jquery',
    'i18n!find/nls/bundle',
    'find/app/configuration',
    'js-whatever/js/list-view',
    'find/app/util/collapsible',
    'find/app/page/search/filters/parametric/parametric-value-view'
], function(Backbone, _, $, i18n, configuration, ListView, Collapsible, ValueView) {

    var ValuesView = Backbone.View.extend({
        className: 'table parametric-fields-table',
        tagName: 'table',

        initialize: function() {
            this.listView = new ListView({
                collection: this.collection,
                tagName: 'tbody',
                ItemView: ValueView,
                collectionChangeEvents: {
                    count: 'updateCount',
                    selected: 'updateSelected'
                }
            });
        },

        render: function() {
            this.$el.empty().append(this.listView.render().$el);
        },

        remove: function() {
            this.listView.remove();
            Backbone.View.prototype.remove.call(this);
        }
    });

    return Backbone.View.extend({
        className: 'animated fadeIn',
        seeMoreButtonTemplate: _.template('<tr class="toggle-more clickable"><td><i class="hp-icon hp-chevron-right"></i></td><td> <span class="toggle-more-text"><%-i18n["app.seeMore"]%></span></td></tr>'),

        events: {
            'click .toggle-more': function(e) {
                this.toggleFacets($(e.currentTarget).hasClass('more'));
            }
        },

        initialize: function() {
            this.$el.attr('data-field', this.model.id);
            this.$el.attr('data-field-display-name', this.model.get('displayName'));
            this.$el.attr('data-numeric-type', this.model.get('numeric'));


            var paramsMap = configuration().parametricDisplayValues;
            var paramMap = _.findWhere(paramsMap, {name: this.model.id});

            if (paramMap) {
                this.model.fieldValues.each(function (value) {
                    var param = _.findWhere(paramMap.values, {name: value.id});
                    if (param) {
                        value.set('displayName', param.displayName);
                    }
                })
            }

            this.collapsible = new Collapsible({
                title: this.model.get('displayName'),
                view: new ValuesView({collection: this.model.fieldValues}),
                collapsed: false
            });
        },

        render: function() {
            this.$el.empty().append(this.collapsible.$el);
            this.collapsible.render();

            if(this.collapsible.$('tbody tr').length > 5) {
                this.collapsible.$('tbody').append(this.seeMoreButtonTemplate({i18n:i18n}));
                this.toggleFacets(true);
            }
        },

        toggleFacets: function(toggle) {
            var lastFacets = this.collapsible.$('tbody tr').slice(5);
            lastFacets.toggleClass('hide', toggle);

            //unhiding see more or see less buttons
            this.$('.toggle-more').removeClass('hide');

            this.collapsible.$('.toggle-more').toggleClass('more', !toggle);
            this.collapsible.$('.toggle-more i').toggleClass('hp-chevron-up', !toggle);
            this.collapsible.$('.toggle-more i').toggleClass('hp-chevron-right', toggle);
            this.collapsible.$('.toggle-more-text').text(toggle ? i18n["app.seeMore"] : i18n["app.seeLess"]);
        },

        remove: function() {
            Backbone.View.prototype.remove.call(this);
            this.collapsible.remove();
        }
    });
});