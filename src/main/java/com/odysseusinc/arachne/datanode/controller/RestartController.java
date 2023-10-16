package com.odysseusinc.arachne.datanode.controller;

import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.WebApplicationStarter;
import io.swagger.annotations.Api;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Api(hidden = true)
@RestController
@Secured("ROLE_ADMIN")
public class RestartController {

    @RequestMapping(value = "/api/v1/admin/restart", method = RequestMethod.POST)
    public JsonResult<?> restart() {
        Thread restartThread = new Thread(WebApplicationStarter::restart);
        restartThread.setDaemon(false);
        restartThread.start();
        return new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
    }

}
