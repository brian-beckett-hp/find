package com.hp.autonomy.frontend.find.hod.web;

import com.hp.autonomy.frontend.configuration.ConfigService;
import com.hp.autonomy.frontend.find.core.web.FindController;
import com.hp.autonomy.frontend.find.hod.configuration.HodFindConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class HodFindController extends FindController {

    @Autowired
    private ConfigService<HodFindConfig> configService;

    @Override
    protected Map<String, Object> getPublicConfig() {
        final HashMap<String, Object> publicMap = new HashMap<>();
        publicMap.put("parametricDisplayValues", configService.getConfig().getParametricDisplayValues());
        return publicMap;
    }
}
