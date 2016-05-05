define([
   'find/app/page/search/filters/parametric/parametric-field-view',
    'find/app/configuration',
    'backbone'
], function(FieldView, configuration, Backbone) {

    describe('Parametric field view', function() {
        beforeEach(function() {
            configuration.and.returnValue({
                parametricDisplayValues: [
                    {
                        name: "place_country_code",
                        values: [
                            {
                                name: "US",
                                displayName: "United States of America"
                            },
                            {
                                name: "UK",
                                displayName: "United Kingdom of Great Britain and Northern Ireland"
                            }
                        ]
                    }]
            });

            this.model = new Backbone.Model({
                displayName: 'Primary Author',
                id: 'primary_author'
            });

            this.model.fieldValues = new Backbone.Collection([
                {id: 'bob', count: 100, selected: true},
                {id: 'penny', count: 96, selected: true},
                {id: 'fred', count: 25, selected: false}
            ]);

            this.fieldView = new FieldView({model: this.model});
            this.fieldView.render();
        });

        it('sets a data-field attribute', function() {
            expect(this.fieldView.$el).toHaveAttr('data-field', 'primary_author');
        });

        it('displays the display name', function() {
            expect(this.fieldView.$el).toContainText('Primary Author');
        });

        it('displays the field values', function() {
            expect(this.fieldView.$('[data-value="bob"]')).toHaveLength(1);
            expect(this.fieldView.$('[data-value="penny"]')).toHaveLength(1);
            expect(this.fieldView.$('[data-value="fred"]')).toHaveLength(1);
        });
    });

});