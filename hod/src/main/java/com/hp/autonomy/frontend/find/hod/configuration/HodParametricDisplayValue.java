package com.hp.autonomy.frontend.find.hod.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
class HodParametricDisplayValue {
    final String name;
    final String displayName;

    public HodParametricDisplayValue(@JsonProperty("name") final String name, @JsonProperty("displayName") final String displayName) {
        this.name = name;
        this.displayName = displayName;
    }
}
