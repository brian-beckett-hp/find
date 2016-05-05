package com.hp.autonomy.frontend.find.hod.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
class HodParametricDisplayValues {
    private final String name;
    private final List<HodParametricDisplayValue> values;

    public HodParametricDisplayValues(@JsonProperty("name") final String name, @JsonProperty("values") final List<HodParametricDisplayValue> values) {
        this.name = name;
        this.values = values;
    }
}
