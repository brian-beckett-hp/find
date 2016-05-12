define([
    'jquery',
    'find/app/configuration'
], function($, configuration) {

    return function() {
        var deferred = $.Deferred();

        $.ajax({
            url: 'https://login.sso.philips.com'
        })
            .done(function() {
                configuration().onPhilipsNetwork = true;
            })
            .error(function() {
                configuration().onPhilipsNetwork = false;
            })
            .always(function () {
                deferred.resolve();
            });

        return deferred.promise();
    }

});
