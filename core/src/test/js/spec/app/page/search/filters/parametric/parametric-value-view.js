define([
    'find/app/page/search/filters/parametric/parametric-value-view',
    'find/app/configuration',
    'backbone'
], function(ValueView, configuration, Backbone) {

    describe('Parametric value view', function() {
        
        afterEach(function() {
            configuration.and.stub();
        });
        
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
                id: 'cat',
                count: 3,
                selected: false
            });

            this.view = new ValueView({model: this.model});
            this.view.render();

            this.$check = this.view.$('.parametric-value-icon');
            this.$text = this.view.$('.parametric-value-text');
            this.$name = this.view.$('.parametric-value-name');
            this.$count = this.view.$('.parametric-value-count');
        });

        it('sets a data-value attribute', function() {
            expect(this.view.$el).toHaveAttr('data-value', 'cat');
        });

        it('displays the value name', function() {
            expect(this.$name).toContainText('cat');
        });

        it('displays the count', function() {
            expect(this.$count).toContainText('(3)');
        });

        it('hides the check icon if the value is not selected', function() {
            expect(this.$check).toHaveClass('hide');
        });

        describe('after the count is set to null', function() {
            beforeEach(function() {
                this.model.set('count', null);
                this.view.updateCount();
            });

            it('displays the value name', function() {
                expect(this.$name).toContainText('cat');
            });

            it('does not display the count', function() {
                expect(this.$count).not.toContainText('(3)');
                expect(this.$count).not.toContainText('(0)');
                expect(this.$count).not.toContainText('()');
                expect(this.$count).not.toContainText('null');
            });

            describe('then the count is set to a number', function() {
                beforeEach(function() {
                    this.model.set('count', 50);
                    this.view.updateCount();
                });

                it('displays the value name', function() {
                    expect(this.$name).toContainText('cat');
                });

                it('displays the count', function() {
                    expect(this.$count).toContainText('(50)');
                });
            });
        });

        describe('after the value is selected', function() {
            beforeEach(function() {
                this.model.set('selected', true);
                this.view.updateSelected();
            });

            it('shows the check icon', function() {
                expect(this.$check).not.toHaveClass('hide');
            });

            describe('then the value is deselected', function() {
                beforeEach(function() {
                    this.model.set('selected', false);
                    this.view.updateSelected();
                });

                it('hides the check icon', function() {
                    expect(this.$check).toHaveClass('hide');
                });
            });
        });
    });

});
