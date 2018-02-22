package com.houoy.game.test.controller;

import com.houoy.common.vo.Result;
import com.houoy.common.vo.ResultCode;
import com.houoy.game.test.vo.BetDetailRecordVO;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/")
public class NetAndSensorController {

    @Autowired
    private RestTemplate restTemplate;

    @ApiOperation(value = "测试批量下注")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pk_period", value = "期pk", required = true, dataType = "string", paramType = "query")
    })
    @GetMapping(value = "/testBetSaveBatch", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Result testBetSaveBatch(@RequestParam("pk_period") String pk_period) {
        HttpHeaders requestHeaders = new HttpHeaders();
//        requestHeaders.add("keyid",urlConfig.getKeyid());
        MediaType type = MediaType.parseMediaType("application/json;charset=UTF-8");
        requestHeaders.setContentType(type);
        //整理查询详细deviceDetailUrl
        String deviceDetailUrl = "http://localhost:8890/api/bet/saveBatch";

        //构建查询设备的参数
        List<BetDetailRecordVO> vos = new ArrayList();
        BetDetailRecordVO vo = new BetDetailRecordVO();
        vo.setPk_user("4");
        vo.setPk_period("1234");
        vo.setBet_item("big");
        vo.setBet_money(100l);
        vos.add(vo);

        HttpEntity<List<BetDetailRecordVO>> requestEntityDeviceDetail = new HttpEntity<List<BetDetailRecordVO>>(vos, requestHeaders);
        ResponseEntity<Result> exchangeDeviceDetail = restTemplate.exchange(deviceDetailUrl,
                HttpMethod.POST, requestEntityDeviceDetail, Result.class);

        Result result = new Result();
        result.setCode(ResultCode.SUCCESS);
        result.setContent(exchangeDeviceDetail);
        result.setMsg("success");
        return result;
    }

/*
    @ApiOperation(value = "新增关联 并将采集点复制保存到db", notes = "新增关联并复制保存所有sensor")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appid", value = "应用id", required = true, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "groupid", value = "机构id", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "userid", value = "用户id", required = true, dataType = "string", paramType = "query"),
    })
    @PostMapping(value = "/A0100101", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Result<String> add(@RequestParam("appid")String appid, String groupid, @RequestParam("userid")String userid, @RequestBody NetworkInVO A0100101InVo){
        log.info("Enter save appid={}，groupid={},userid={},A0100101InVo={}",appid,groupid,userid,A0100101InVo);
        Result<String> result = new Result<String>(ResultCode.ERROR_PARAMETER,"保存失败",null);
        if(TextUtils.isEmpty(userid)){
            log.info("The userid is null");
            result.setMsg("用户id不能为空");
            return result;
        }
        if(TextUtils.isEmpty(appid)){
            log.info("The appid is null");
            result.setMsg("应用id不能为空");
            return result;
        }
        if(CodeUtil.isNull(A0100101InVo)){
            log.info("The A0100101InVo is null");
            result.setMsg("参数不能为空");
            return result;
        }
        try {
            //调用平台新建组网指令
            HttpHeaders requestHeaders = new HttpHeaders();
            //设置请求头
            requestHeaders.add("keyid", urlConfig.getKeyid());
            MediaType type = MediaType.parseMediaType("application/json;charset=UTF-8");
            requestHeaders.setContentType(type);
            String netCreateUrl = urlConfig.getNetCreateUrl()+"?userid="+userid+"&appid="+appid;
            if(CodeUtil.isNotNullEmpty(groupid)){
                netCreateUrl = netCreateUrl + "&groupid="+groupid;
            }
            //设置参数
            HttpEntity<NetworkInVO> requestEntity = new HttpEntity<NetworkInVO>(A0100101InVo, requestHeaders);
            //发送指令
            ResponseEntity<Result> exchange = restTemplate.exchange(netCreateUrl,
                    HttpMethod.POST, requestEntity, Result.class);
            //获取返回值
            Result value = exchange.getBody();
            if(value != null && value.getCode().intValue() == 0){
                try {
                    List<Map<String,String>> modelidAndDeviceidList = A0100101InVo.getMapList();
                    this.sensorService.setSensorTemplateOver(null,modelidAndDeviceidList);
                    result.setContent(value.getContent().toString());
                    result.setMsg("保存成功");
                    result.setCode(ResultCode.SUCCESS);
                } catch (Exception e) {
                    log.error("The netAndSensor sensors save to DB error",e);
                    try {
                        String deviceDelRelationUrl = urlConfig.getDeviceDelRelationUrl()+"?userid="+userid+"&appid="+appid;
                        if(CodeUtil.isNotNullEmpty(groupid)){
                            deviceDelRelationUrl = deviceDelRelationUrl + "&groupid="+groupid;
                        }
                        DynamicRelationElement dynamicRelationElement = new DynamicRelationElement();
                        dynamicRelationElement.setIdField("");
                        dynamicRelationElement.setIdValue(value.getContent().toString());

                        DynamicRelationInVO dynamicRelationInVO = new DynamicRelationInVO();
                        dynamicRelationInVO.setBegin(dynamicRelationElement);
                        dynamicRelationInVO.setEnds(A0100101InVo.getDynamicRelationInVO().getEnds());

                        //解除组网与设备的关系
                        //设置参数
                        HttpEntity<DynamicRelationInVO> requestEntityDelRelation = new HttpEntity<DynamicRelationInVO>(dynamicRelationInVO, requestHeaders);
                        //发送指令
                        ResponseEntity<Result> exchangeDelRelation = restTemplate.exchange(deviceDelRelationUrl,
                                HttpMethod.POST, requestEntityDelRelation, Result.class);
                        //设置请求参数
                        List<String> stringList = new ArrayList<String>();
                        stringList.add(value.getContent().toString());
                        //设置请求url
                        String netDeleteUrl = urlConfig.getNetDeleteUrl()+"?userid="+userid+"&appid="+appid;
                        if(CodeUtil.isNotNullEmpty(groupid)){
                            netDeleteUrl = netDeleteUrl + "&groupid="+groupid;
                        }
                        //设置参数
                        HttpEntity<List<String>> requestEntityDelete = new HttpEntity<List<String>>(stringList, requestHeaders);
                        //发送指令
                        ResponseEntity<Result> exchangeDelete = restTemplate.exchange(netDeleteUrl,
                                HttpMethod.DELETE, requestEntityDelete, Result.class);
                    } catch (RestClientException e1) {
                        log.info("The dpm-service net relieve roll-back fail",e1);
                        result.setMsg("远端回滚失败");
                    }
                }
            }
        } catch (RestClientException e) {
            log.info("The net save call dpm-service netCreate error",e);
            result.setMsg("远端调用失败");
        }
        log.info("Leave net save result={}", result);
        return result;
    }

    *//**
     *  获取解除组网参数
     * @param paramMap
     * @return
     *//*
    private List<String> getNetRelieveDeviceids(List<Map<String,String>> paramMap){
        List<String> stringList = new ArrayList<String>();
        for(Map<String,String> map : paramMap){
            stringList.add(map.get("deviceid"));
        }
        return stringList;
    }

    @ApiOperation(value = "修改关联 原采集点删除，建新采集点", notes = "修改组网 原采集点删除，建新采集点")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appid", value = "应用id", required = true, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "groupid", value = "机构id", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "userid", value = "用户id", required = true, dataType = "string", paramType = "query")
    })
    @PutMapping(value = "/A0100102", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Result<Boolean> update(@RequestParam("appid")String appid,String groupid,@RequestParam("userid")String userid, @RequestBody NetworkInVO A0100102InVo){
        log.info("Enter save appid={}，groupid={},userid={},A0100102InVo={}",appid,groupid,userid,A0100102InVo);
        Result<Boolean> result = new Result<Boolean>(ResultCode.ERROR_PARAMETER,"修改失败",false);
        //-------- 参数校验
        if(TextUtils.isEmpty(appid)){
            log.info("The appid is null");
            result.setMsg("应用信息不能为空");
            return result;
        }
        if(TextUtils.isEmpty(userid)){
            log.info("The userid is null");
            result.setMsg("用户信息不能为空");
            return result;
        }
        if(CodeUtil.isNull(A0100102InVo)){
            log.info("The networkInVO is null");
            result.setMsg("参数不能为空");
            return result;
        }
        String networkId = A0100102InVo.getNetworkid();
        if(CodeUtil.isNotNullEmpty(networkId)){
            // 查询组网详细
            try {
                HttpHeaders requestHeaders = new HttpHeaders();
                requestHeaders.add("keyid",urlConfig.getKeyid());
                MediaType type = MediaType.parseMediaType("application/json;charset=UTF-8");
                requestHeaders.setContentType(type);
                //整理查询详细deviceDetailUrl
                String deviceDetailUrl = urlConfig.getDeviceRetrieveUrl()+"?id="+networkId+"&appid="+appid;
                if(CodeUtil.isNotNullEmpty(userid)){
                    deviceDetailUrl = deviceDetailUrl + "&userid="+userid;
                }
                if(CodeUtil.isNotNullEmpty(groupid)){
                    deviceDetailUrl = deviceDetailUrl + "&groupid="+groupid;
                }
                     //构建查询设备的参数
                DynamicRetrieveInVO dynamicRetrieveInVO = new DynamicRetrieveInVO();
                List<DynamicRetrieveElement> dynamicRetrieveElements = new ArrayList<DynamicRetrieveElement>();
                List<DynamicRetrieveElementCondition> dynamicRetrieveElementConditions = new ArrayList<DynamicRetrieveElementCondition>();
                DynamicRetrieveElement dynamicRetrieveElement = new DynamicRetrieveElement();
                DynamicRetrieveElementCondition dynamicRetrieveElementCondition = new DynamicRetrieveElementCondition();
                dynamicRetrieveElement.setName("networkid");
                dynamicRetrieveElementCondition.setOpt("=");
                dynamicRetrieveElementCondition.setValue(networkId);
                dynamicRetrieveElementConditions.add(dynamicRetrieveElementCondition);
                dynamicRetrieveElement.setCondition(dynamicRetrieveElementConditions);
                dynamicRetrieveElements.add(dynamicRetrieveElement);
                dynamicRetrieveInVO.setQueryElement(dynamicRetrieveElements);

                HttpEntity<DynamicRetrieveInVO> requestEntityDeviceDetail = new HttpEntity<DynamicRetrieveInVO>(dynamicRetrieveInVO, requestHeaders);
                ResponseEntity<Result> exchangeDeviceDetail = restTemplate.exchange(deviceDetailUrl,
                        HttpMethod.POST, requestEntityDeviceDetail, Result.class);
                Result valueDeviceDetail = exchangeDeviceDetail.getBody();

                //查询组网下设备详细成功
                if(CodeUtil.isNotNull(valueDeviceDetail)){
                    if(valueDeviceDetail.getCode().intValue() == 0){
                        //整理查询详细detailUrl
                        String netDetailUrl = urlConfig.getNetWorkDetailUrl()+"?id="+networkId+"&appid="+appid;
                        if(CodeUtil.isNotNullEmpty(userid)){
                            netDetailUrl = netDetailUrl + "&userid="+userid;
                        }
                        if(CodeUtil.isNotNullEmpty(groupid)){
                            netDetailUrl = netDetailUrl + "&groupid="+groupid;
                        }
                        HttpEntity<String> requestEntityNetDetail = new HttpEntity<String>(null, requestHeaders);
                        ResponseEntity<Result> exchangeNetDetail = restTemplate.exchange(netDetailUrl,
                                HttpMethod.GET, requestEntityNetDetail, Result.class);
                        Result valueNetDetail = exchangeNetDetail.getBody();
                        //查询组网详细成功
                        if(CodeUtil.isNotNull(valueNetDetail)){
                            if(valueNetDetail.getCode().intValue() == 0){
                                //获取设备id并组装成需要的形式
                                Object networkObj = valueDeviceDetail.getContent();
                                if(CodeUtil.isNotNull(networkObj)){
                                    //获取设备id 并拼接成想要的格式
                                    Map<String,Object> map = (Map<String,Object>) networkObj;
                                    if(CodeUtil.isNotNull(map) && map.size()>0){
                                        Object devices = map.get("rows");
                                        if(CodeUtil.isNotNull(devices)) {
                                            List<Map<String, Object>> oldDeviceListMaps = (List<Map<String, Object>>) devices;
                                            List<Map<String, String>> paramListMaps = A0100102InVo.getMapList();
                                            //判断整理设备 变化
                                            formatList(oldDeviceListMaps,paramListMaps);

                                            String oldDeviceids = getUpdateDeviceids(oldDeviceListMaps);
                                            try {
                                                String updateWithDeviceUrl = urlConfig.getUpdateWithDeviceUrl()+"?userid="+userid+"&appid="+appid;
                                                if(CodeUtil.isNotNullEmpty(groupid)){
                                                    updateWithDeviceUrl = updateWithDeviceUrl + "&groupid="+groupid;
                                                }
                                                //调用远端进行组网修改
                                                HttpEntity<NetworkInVO> requestEntityUpdate = new HttpEntity<NetworkInVO>(A0100102InVo, requestHeaders);
                                                ResponseEntity<Result> exchangeUpdate = restTemplate.exchange(updateWithDeviceUrl,
                                                        HttpMethod.PUT, requestEntityUpdate, Result.class);
                                                Result valueUpdate = exchangeUpdate.getBody();
                                                if(CodeUtil.isNotNull(valueUpdate) && valueUpdate.getCode() == 0){//平台组网修改成功
                                                    try {
                                                        //采集点的复制
                                                        this.sensorService.setSensorTemplateOver(oldDeviceids,paramListMaps);
                                                        result.setMsg("修改成功");
                                                        result.setCode(ResultCode.SUCCESS);
                                                        result.setContent(true);
                                                    } catch (Exception e) {
                                                        log.error("The save sensors to DB fail",e);
                                                        result.setMsg("组网修改失败");
                                                        try {
                                                            //采集点复制失败 平台远端进行回滚
                                                            HttpEntity<String> requestEntityRollUpdate = new HttpEntity<String>("", requestHeaders);
                                                            ResponseEntity<Boolean> exchangeRollUpdate = restTemplate.exchange(updateWithDeviceUrl,
                                                                    HttpMethod.PUT, requestEntityRollUpdate, Boolean.class);
                                                            Boolean valueRollUpdate = exchangeRollUpdate.getBody();
                                                            if(!valueRollUpdate){
                                                                result.setMsg("远端回滚失败");
                                                                return result;
                                                            }
                                                        } catch (RestClientException e1) {
                                                            log.error("The dpm-service net update roll back fail",e1);
                                                            result.setMsg("远端回滚失败");
                                                            return result;
                                                        }
                                                    }
                                                }else{ // 远端修改失败
                                                    log.info("The dpm-service net update fail");
                                                    result.setMsg("远端修改失败");
                                                    return result;
                                                }
                                            } catch (RestClientException e) {//平台远端调用失败 或 修改失败
                                                log.info("The dpm-service net update fail");
                                                result.setMsg("远端调用失败");
                                                return result;
                                            }
                                        }
                                    }

                                }
                            }else{
                                //返回查询详细的失败原因
                                result.setMsg(valueNetDetail.getMsg());
                                return result;
                            }
                        }else{//查询组网详细失败
                            log.info("The dpm-service netWork detail no content");
                            result.setMsg("远端调用失败");
                            return result;
                        }
                    }else{
                        //返回查询详细的失败原因
                        result.setMsg(valueDeviceDetail.getMsg());
                        return result;
                    }
                }else{//查询组网详细失败
                    log.info("The dpm-service netWork detail no content");
                    result.setMsg("远端调用失败");
                    return result;
                }
            } catch (RestClientException e) {//查询组网详细远端调用失败
                log.error("The update call dpm-service net findById fail",e);
                result.setMsg("远端调用失败");
                return result;
            }
        }else{
            result.setMsg("组网id为空");
        }
        log.info("Leave update result={}", result);
        return result;
    }

    *//**
     *  获取解除组网设备id
     * @param paramList
     * @return
     *//*
    private String getUpdateDeviceids(List<Map<String, Object>> paramList){
        StringBuffer sb = new StringBuffer("");
        String deviceids = "";
        for(Map<String, Object> map : paramList){
            sb.append("'").append(map.get("deviceid")).append("'").append(",");
        }
        if(sb.toString().endsWith(",")){
            deviceids = sb.substring(0,sb.length()-1);
        }
        return deviceids;
    }

    @ApiOperation(value = "解除关联并删除相关采集点", notes = "解除组网并删除相关采集点")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appid", value = "应用id", required = true, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "groupid", value = "机构id", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "userid", value = "用户id", required = true, dataType = "string", paramType = "query")
    })
    @DeleteMapping(value = "/A0100103", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Result<Boolean> del(@RequestParam("appid")String appid,String groupid,@RequestParam("userid")String userid, @RequestBody DynamicRelationInVO A0100103InVo){
        log.info("Enter del appid={}，groupid={},userid={},A0100103InVo={}",appid,groupid,userid,A0100103InVo);
        Result<Boolean> result = new Result<Boolean>(ResultCode.ERROR_PARAMETER,"解除失败",false);
        //-------- 参数校验
        if(TextUtils.isEmpty(appid)){
            log.info("The appid is null");
            result.setMsg("应用信息不能为空");
            return result;
        }
        if(TextUtils.isEmpty(userid)){
            log.error("The userid is null");
            result.setMsg("用户信息不能为空");
            return result;
        }
        if(!CodeUtil.isNotNull(A0100103InVo)){
            log.error("The A0100103InVo is null");
            result.setMsg("参数不能为空");
            return result;
        }
        String deviceids = getDelDeviceids(A0100103InVo.getEnds());
        //校验采集点是否已绑定监控点
        try {
            List<String> stringList = new ArrayList<String>();
            stringList.add(A0100103InVo.getBegin().getIdValue());
            //调用平台解除组网关系
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.add("keyid",urlConfig.getKeyid());
            MediaType type = MediaType.parseMediaType("application/json;charset=UTF-8");
            requestHeaders.setContentType(type);
            //首先删出设备与组网关系
            String deviceDelRelationUrl = urlConfig.getDeviceDelRelationUrl()+"?userid="+userid+"&appid="+appid;
            if(CodeUtil.isNotNullEmpty(groupid)){
                deviceDelRelationUrl = deviceDelRelationUrl + "&groupid="+groupid;
            }
            HttpEntity<DynamicRelationInVO> requestEntity = new HttpEntity<DynamicRelationInVO>(A0100103InVo, requestHeaders);
            ResponseEntity<Result> exchange = restTemplate.exchange(deviceDelRelationUrl,
                    HttpMethod.POST, requestEntity, Result.class);
            Result valueDel = exchange.getBody();

            if(CodeUtil.isNotNull(valueDel) && valueDel.getCode().intValue() == 0){
                //设置请求url
                String netBuryUrl = urlConfig.getNetBuryUrl()+"?userid="+userid+"&appid="+appid;
                if(CodeUtil.isNotNullEmpty(groupid)){
                    netBuryUrl = netBuryUrl + "&groupid="+groupid;
                }
                HttpEntity<List<String>> requestEntityNet = new HttpEntity<List<String>>(stringList, requestHeaders);
                ResponseEntity<Result> exchangeNet = restTemplate.exchange(netBuryUrl,
                        HttpMethod.DELETE, requestEntityNet, Result.class);
                Result valueDelNet = exchangeNet.getBody();
                if(CodeUtil.isNotNull(valueDelNet) && valueDelNet.getCode().intValue() == 0){
                    this.sensorService.delSensorsByDeviceids(deviceids);
                    result.setMsg("解除成功");
                    result.setCode(ResultCode.SUCCESS);
                    result.setContent(true);
                }else{
                    result.setMsg("远端调用失败");
                    return result;
                }
            }
        } catch (RestClientException e) {
            e.printStackTrace();
            log.info("The del call dpm-service net bury fail");
            result.setMsg("远端调用失败");
            return result;
        }
        return result;
    }

    *//**
     *  获取解除组网设备id
     * @param dynamicRelationElements
     * @return
     *//*
    private String getDelDeviceids(List<DynamicRelationElement> dynamicRelationElements){
        StringBuffer sb = new StringBuffer("");
        for(DynamicRelationElement dynamicRelationElement : dynamicRelationElements){
            sb.append("'").append(dynamicRelationElement.getIdValue()).append("'").append(",");
        }
        String deviceids = sb.toString();
        if(deviceids.endsWith(",")){
            deviceids = deviceids.substring(0,deviceids.length()-1);
        }
        return deviceids;
    }

    *//**
     * 格式化修改组网数据 将组网新增的设备 与 删除的设备 分开提取出来
     * @param oldDeviceListMaps
     * @param paramListMaps
     *//*
    private void formatList( List<Map<String, Object>> oldDeviceListMaps, List<Map<String, String>> paramListMaps){
        List<Integer> paramNoChangeNumList = new ArrayList<Integer>();
        TreeSet<Integer> oldNoChangeNumList = new TreeSet<Integer>();
        for(int i=0;i< paramListMaps.size();i++){
            String deviceid = paramListMaps.get(i).get("deviceid");
            for(int j=0;j< oldDeviceListMaps.size();j++){
                String oldDevice = oldDeviceListMaps.get(j).get("deviceid").toString();
                if(deviceid.equals(oldDevice)){
                    paramNoChangeNumList.add(i);
                    oldNoChangeNumList.add(j);
                    break;
                }
            }
        }
        if(CodeUtil.isNotNullZero(paramNoChangeNumList)){
            int z=0;
            for(Integer  paramNoChangeNum: paramNoChangeNumList){
                paramListMaps.remove(paramNoChangeNum.intValue()-z);
                z++;
            }
        }
        if(oldNoChangeNumList !=null && oldNoChangeNumList.size()>0){
            int y=0;
            for(Integer oldNoChangeNum : oldNoChangeNumList){
                oldDeviceListMaps.remove(oldNoChangeNum.intValue()-y);
                y++;
            }
        }
    }*/
}
