<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.engine.kq.enums.KqSplitFlowTypeEnum,com.engine.kq.wfset.attendance.domain.WorkflowBase,com.engine.kq.wfset.attendance.manager.WorkflowBaseManager,com.engine.kq.wfset.util.KQAttFlowCheckUtil" %>
<%@ page import="java.util.Map"%>
<%@page import="weaver.general.Util"%>
<%@ page import="weaver.hrm.User" %>
<%@ page import="weaver.systeminfo.SystemEnv" %>
<%@ page import="weaver.general.TimeUtil" %>
<jsp:useBean id="strUtil" class="weaver.common.StringUtil" scope="page" />
<jsp:useBean id="dateUtil" class="weaver.common.DateUtil" scope="page" />
<jsp:useBean id="RecordSet" class="weaver.conn.RecordSet" scope="page" />
<jsp:useBean id="attProcSetManager" class="com.engine.kq.wfset.attendance.manager.HrmAttProcSetManager" scope="page" />
<jsp:useBean id="KQBalanceOfLeaveBiz" class="com.engine.kq.biz.KQBalanceOfLeaveBiz" scope="page" />
<%
    User user = (User)request.getSession(true).getAttribute("weaver_user@bean");
    int nodetype = Util.getIntValue(request.getParameter("nodetype"), 0);
    int workflowid = Util.getIntValue(request.getParameter("workflowid"), 0);
    int nodeid = Util.getIntValue(request.getParameter("nodeid"), 0);
    int formid = Util.getIntValue(request.getParameter("formid"));
    int userid = Util.getIntValue(request.getParameter("userid"));
    int requestid = Util.getIntValue(request.getParameter("requestid"));
    String creater = Util.null2s(request.getParameter("creater"), String.valueOf(userid));
    WorkflowBaseManager workflowBaseManager = new WorkflowBaseManager();
    if(formid == -1) {
        WorkflowBase bean = workflowBaseManager.get(workflowid);
        formid = bean == null ? -1 : bean.getFormid();
    }
    Map<String,Object> result = attProcSetManager.getFieldList(0,workflowid, formid);
    String[] fieldList = (String[])result.get("fields");
    String usedetail = Util.null2String(result.get("usedetail"));
    String detailtablename = Util.null2String(result.get("detailtablename"));
    //考勤流程设置id
    String attid = Util.null2String(result.get("attid"));

    String isAttOk = "1";
    String msgAttError = "";
    boolean isKQFlow = KQAttFlowCheckUtil.isKQFlow(result);
    if(!isKQFlow){
        return ;
    }
    Map<String,String> check = KQAttFlowCheckUtil.checkAttFlow(result, KqSplitFlowTypeEnum.LEAVE);
    isAttOk = Util.null2s(check.get("isAttOk"),"0");
    msgAttError = Util.null2s(check.get("msgAttError"),"考勤流程设置有误");
    int _customAddFun = 0;
    String detail_dt = "1";
    int detailIndex = Util.getIntValue(Util.null2String(result.get("detailIndex")),1);
    int tablelength = detailtablename.length();
    int index_dt = detailtablename.indexOf("dt");
    if(tablelength>0 && index_dt>0){
        detailIndex = Util.getIntValue(Util.null2String(detailtablename.substring(index_dt+2,tablelength)),1);
    }
    if(detailIndex > 0){
        detail_dt = ""+detailIndex;
        _customAddFun = Util.getIntValue(detail_dt)-1;
    }

    String currentdate = Util.null2s(request.getParameter("currentdate"), dateUtil.getCurrentDate());
    String f_weaver_belongto_userid = Util.null2s(request.getParameter("f_weaver_belongto_userid"),"");
    String f_weaver_belongto_usertype = Util.null2s(request.getParameter("f_weaver_belongto_usertype"),"");
    String curDate = TimeUtil.getCurrentDateString();

    String currentnodetype = "";
    String show_ajax_balance = "1";
    String show_ajax_balance_sql = "select * from kq_settings where main_key='show_ajax_balance'";
    RecordSet.executeQuery(show_ajax_balance_sql);
    if(RecordSet.next()) {
        String main_val = RecordSet.getString("main_val");
        if ("0".equalsIgnoreCase(main_val)) {
            show_ajax_balance = "0";
        }
    }
    if(requestid > 0){
        String sql = "select currentnodetype from workflow_requestbase where requestid = "+requestid;
        RecordSet.executeQuery(sql);
        if(RecordSet.next()){
            currentnodetype = RecordSet.getString("currentnodetype");
        }
    }else{
        currentnodetype = nodetype+"";
    }
%>
<script  src="<%=weaver.general.GCONST.getContextPath()%>/workflow/request/ext4e9/common.js"></script>
<script >
    var isMobile = WfForm.isMobile();
    var isAttOk = "<%=isAttOk%>";
    var usedetail = "<%=usedetail%>";
    var detail_dt = "<%=detail_dt%>";
    var _customAddFun = "<%=_customAddFun%>";
    var formid = "<%=formid%>";
    var workflowid = "<%=workflowid%>";
    var nodeid = "<%=nodeid%>";
    var _field_resourceId = "<%=fieldList[0]%>";
    var _field_newLeaveType = "<%=fieldList[2]%>";
    var _field_fromDate = "<%=fieldList[3]%>";
    var _field_fromTime = "<%=fieldList[4]%>";
    var _field_toDate = "<%=fieldList[5]%>";
    var _field_toTime = "<%=fieldList[6]%>";
    var _field_duration = "<%=fieldList[7]%>";
    var _field_vacationInfo = "<%=fieldList[8]%>";
    var f_weaver_belongto_userid = "<%=f_weaver_belongto_userid%>";
    var f_weaver_belongto_usertype = "<%=f_weaver_belongto_usertype%>";
    var msgAttError = "<%=msgAttError%>";
    var min_duration = "";
    var requestid = "<%=requestid%>";
    var currentnodetype = "<%=currentnodetype%>";
    var show_ajax_balance = "<%=show_ajax_balance%>";
    var attid = "<%=attid%>";
    //避免频繁ajax请求的时间戳对象
    var _duration_stamp = {};

    jQuery(document).ready(function(){
        try{

            if(usedetail != "1"){
                if(_field_resourceId != "") {
                    try{
                        getVacationInfo();
                        WfForm.changeFieldAttr(_field_vacationInfo, 1);
                    }catch(e){}
                }
                if(_field_duration != "") {
                    try{
                        WfForm.changeFieldAttr(_field_duration, 1);
                    }catch(e){}
                }
            }else{
                var detailAllRowIndexStr = WfForm.getDetailAllRowIndexStr("detail_"+detail_dt);
                if (detailAllRowIndexStr != "") {
                    var detailAllRowIndexStr_array = detailAllRowIndexStr.split(",");
                    for (var rowIdx = 0; rowIdx < detailAllRowIndexStr_array.length; rowIdx++) {
                        var idx = detailAllRowIndexStr_array[rowIdx];
                        getVacationInfo("","",idx);
                        WfForm.changeFieldAttr(_field_duration+"_"+idx, 1);
                        WfForm.changeFieldAttr(_field_vacationInfo+"_"+idx, 1);
                    }
                }
            }
            var changeFields =_field_resourceId+","+_field_newLeaveType+","+_field_fromDate+","+_field_fromTime
                +","+_field_toDate+","+_field_toTime;

            if(usedetail == "1"){
                WfForm.bindDetailFieldChangeEvent(changeFields, function(id,rowIndex,value){
                    _wfbrowvalue_onchange_detail(id,rowIndex,value);
                });
            }else{
                WfForm.bindFieldChangeEvent(changeFields, function(obj,id,value){
                    _wfbrowvalue_onchange(obj,id,value);
                });
            }

            //绑定提交前事件
            WfForm.registerCheckEvent(WfForm.OPER_SUBMIT+","+WfForm.OPER_SUBMITCONFIRM,function(callback){
                doBeforeSubmit_hrm(callback);
            });

            if(usedetail == "1"){
                var f = "_customAddFun"+_customAddFun;
                window[f] = function (addIndexStr) {
                    if(addIndexStr !=undefined && addIndexStr != null){
                        WfForm.changeFieldAttr(_field_duration+"_"+addIndexStr, 1);
                        WfForm.changeFieldAttr(_field_vacationInfo+"_"+addIndexStr, 1);
                    }
                }

            }
        }catch (e) {
        }

    });


    /**
     * 明细字段变化触发事件
     * @param obj
     * @param fieldid
     * @param rowindex
     * @private
     */
    function _wfbrowvalue_onchange_detail(id, rowIndex, value) {
        if(id == _field_newLeaveType || id == _field_vacationInfo){
            getVacationInfo(id,value,rowIndex);
        }else if(id == _field_fromDate || id == _field_fromTime || id == _field_toDate || id == _field_toTime){
            getLoop_WorkDuration(rowIndex,0,[]);
            if(id == _field_fromDate){
                getVacationInfo(id,value,rowIndex);
            }
        }else if(id == _field_resourceId){
            getVacationInfo(id,value,rowIndex);
        }
    }
    /**
     * 字段变化触发事件
     * @param obj
     * @param fieldid
     * @param rowindex
     * @private
     */
    function _wfbrowvalue_onchange(obj,id,value) {
        if(id == _field_newLeaveType || id == _field_vacationInfo){
            getVacationInfo("",value,"");
        }else if(id == _field_fromDate || id == _field_fromTime || id == _field_toDate || id == _field_toTime){
            getLoop_WorkDuration("",0,[]);
            if(id == _field_fromDate){
                getVacationInfo("",value,"");
            }
        }else if(id == _field_resourceId){
            getVacationInfo("",value,"");
        }
    }

    function getVacationInfo(id,value,rowIndex){
        if(show_ajax_balance == "0"){
            if(currentnodetype == 3){
                return ;
            }
        }
        var _field_field_resourceId = _field_resourceId;
        var _field_field_newLeaveType = _field_newLeaveType;
        var _field_field_vacationInfo = _field_vacationInfo;
        var _field_field_field_fromDate = _field_fromDate;
        var _field_field_fromTime = _field_fromTime;
        var _field_field_toTime = _field_toTime;
        if(rowIndex !=undefined && rowIndex != null && rowIndex != ""){
            _field_field_resourceId += "_"+rowIndex;
            _field_field_newLeaveType += "_"+rowIndex;
            _field_field_vacationInfo += "_"+rowIndex;
            _field_field_field_fromDate += "_"+rowIndex;
            _field_field_fromTime += "_"+rowIndex;
            _field_field_toTime += "_"+rowIndex;
        }
        var _newLeaveTypeVal = null2String(WfForm.getFieldValue(_field_field_newLeaveType));
        var _resourceIdVal = null2String(WfForm.getFieldValue(_field_field_resourceId));
        var _fromDateVal = null2String(WfForm.getFieldValue(_field_field_field_fromDate));
        var _field_fromTimeVal = null2String(WfForm.getFieldValue(_field_field_fromTime));
        var _field_toTimeVal = null2String(WfForm.getFieldValue(_field_field_toTime));

        if(!_newLeaveTypeVal || !_resourceIdVal) return ;

        var _data = "newLeaveType="+_newLeaveTypeVal+"&resourceId="+_resourceIdVal+"&fromDate="+_fromDateVal+"&fromTime="+_field_fromTimeVal+"&toTime="+_field_toTimeVal;

        jQuery.ajax({
            url : "<%=weaver.general.GCONST.getContextPath()%>/api/hrm/kq/attendanceEvent/getVacationInfo",
            type : "post",
            processData : false,
            data : _data,
            dataType : "json",
            success: function do4Success(data){
                if(data != null && data.status == "1"){
                    var _key = _field_field_vacationInfo;
                    var _val_json = {};
                    var _viewAttr_json = {};
                    _val_json[_key] = {value:data.vacationInfo};
                    _viewAttr_json[_key] = {viewAttr:1};
                    WfForm.changeMoreField(_val_json,_viewAttr_json);
                    if(data.needClear){
                        if("1" == data.needClear){
                            WfForm.changeFieldValue(_field_field_fromTime, {value:""});
                            WfForm.changeFieldValue(_field_field_toTime, {value:""});
                        }
                    }
                    getLoop_WorkDuration(rowIndex,0,[]);
                }else if(data.status == "2"){
                    var _key = _field_field_vacationInfo;
                    var _val_json = {};
                    var _viewAttr_json = {};
                    _val_json[_key] = {value:''};
                    _viewAttr_json[_key] = {viewAttr:1};
                    WfForm.changeMoreField(_val_json,_viewAttr_json);
                    if(data.needClear){
                        if("1" == data.needClear){
                            WfForm.changeFieldValue(_field_field_fromTime, {value:""});
                            WfForm.changeFieldValue(_field_field_toTime, {value:""});
                        }
                    }
                    getLoop_WorkDuration(rowIndex,0,[]);
                }
            }
        });
    }

    function getLoop_WorkDuration(rowIndex,identity,detailAllRowIndexStr_array){

        var _field_field_newLeaveType = _field_newLeaveType;
        var _field_field_resourceId = _field_resourceId;
        var _field_field_fromDate = _field_fromDate;
        var _field_field_fromTime = _field_fromTime;
        var _field_field_toDate = _field_toDate;
        var _field_field_toTime = _field_toTime;
        var _field_field_duration = _field_duration;
        if(rowIndex !=undefined && rowIndex != null && rowIndex != ""){
            _field_field_newLeaveType += "_"+rowIndex;
            _field_field_resourceId += "_"+rowIndex;
            _field_field_fromDate += "_"+rowIndex;
            _field_field_fromTime += "_"+rowIndex;
            _field_field_toDate += "_"+rowIndex;
            _field_field_toTime += "_"+rowIndex;
            _field_field_duration += "_"+rowIndex;
        }
        var _newLeaveTypeVal = null2String(WfForm.getFieldValue(_field_field_newLeaveType));
        var _field_resourceIdVal = null2String(WfForm.getFieldValue(_field_field_resourceId));
        var _field_fromDateVal = null2String(WfForm.getFieldValue(_field_field_fromDate));
        var _field_fromTimeVal = null2String(WfForm.getFieldValue(_field_field_fromTime));
        var _field_toDateVal = null2String(WfForm.getFieldValue(_field_field_toDate));
        var _field_toTimeVal = null2String(WfForm.getFieldValue(_field_field_toTime));

        if(currentnodetype == 3){
            return ;
        }

        if(!_field_resourceIdVal || !_field_fromDateVal) return ;

        var duration_stamp = new Date().getTime();
        _duration_stamp[_field_field_duration] = duration_stamp;

        var _data = "newLeaveType="+_newLeaveTypeVal+"&resourceId="+_field_resourceIdVal+"&fromDate="+_field_fromDateVal
            +"&fromTime="+_field_fromTimeVal+"&toDate="+_field_toDateVal+"&toTime="+_field_toTimeVal+"&timestamp="+duration_stamp;

        jQuery.ajax({
            url : "<%=weaver.general.GCONST.getContextPath()%>/api/hrm/kq/attendanceEvent/getLeaveWorkDuration",
            type : "post",
            processData : false,
            data : _data,
            dataType : "json",
            success: function do4Success(data){
                if(identity < detailAllRowIndexStr_array.length){
                    getLoop_WorkDuration(detailAllRowIndexStr_array[identity+1],identity+1,detailAllRowIndexStr_array);
                }
                if(data != null && data.status == "1"){
                    var result_timestamp = data.timestamp;
                    if(_duration_stamp && _duration_stamp[_field_field_duration]){
                        if(result_timestamp < _duration_stamp[_field_field_duration]){
                            //如果频繁发送ajax请求，以最后一次的ajax请求为准
                        }else{
                            var _key = _field_field_duration;
                            var _val_json = {};
                            var _viewAttr_json = {};
                            _val_json[_key] = {value:data.duration};
                            _viewAttr_json[_key] = {viewAttr:1};
                            WfForm.changeMoreField(_val_json,_viewAttr_json);
                            min_duration = data.min_duration;
                        }
                    }else{
                        var _key = _field_field_duration;
                        var _val_json = {};
                        var _viewAttr_json = {};
                        _val_json[_key] = {value:data.duration};
                        _viewAttr_json[_key] = {viewAttr:1};
                        WfForm.changeMoreField(_val_json,_viewAttr_json);
                        min_duration = data.min_duration;
                    }
                }else{
					  var _key = _field_field_duration;
                      var _val_json = {};
                      var _viewAttr_json = {};
                      _val_json[_key] = {value:"0"};
                      _viewAttr_json[_key] = {viewAttr:1};
                      WfForm.changeMoreField(_val_json,_viewAttr_json);
                      min_duration = data.min_duration;			
                    if(data.message){
                        WfForm.showMessage(data.message);
                    }
                }
            }
        });
    }

    //提交事件前出发函数
    function doBeforeSubmit_hrm(callback){
        try{
            WfForm.controlBtnDisabled(true);//把流程中的按钮置灰

            if("0" == isAttOk){
                WfForm.showMessage(msgAttError);
                WfForm.controlBtnDisabled(false);
                return;
            }

            var resMap = {};
            var daysMap = {};
            var otherMap = {};
            var checkRuleData = {};
            var checkDurationData = {};

            if(usedetail == "1"){
                var detailAllRowIndexStr = WfForm.getDetailAllRowIndexStr("detail_"+detail_dt);
                if(detailAllRowIndexStr != ""){
                    var detailAllRowIndexStr_array = detailAllRowIndexStr.split(",");
                    for(var rowIdx = 0; rowIdx < detailAllRowIndexStr_array.length; rowIdx++){
                        var idx = detailAllRowIndexStr_array[rowIdx];
                        var _field_resourceId_val = WfForm.getFieldValue(_field_resourceId+"_"+idx);
                        var _field_newLeaveType_val = WfForm.getFieldValue(_field_newLeaveType+"_"+idx);
                        var _field_fromDate_val = WfForm.getFieldValue(_field_fromDate+"_"+idx);
                        var _field_fromTime_val = WfForm.getFieldValue(_field_fromTime+"_"+idx);
                        var _field_toDate_val = WfForm.getFieldValue(_field_toDate+"_"+idx);
                        var _field_toTime_val = WfForm.getFieldValue(_field_toTime+"_"+idx);
                        var _field_duration_val = WfForm.getFieldValue(_field_duration+"_"+idx);
                        if(!DateCheck(_field_fromDate_val,_field_fromTime_val,_field_toDate_val,_field_toTime_val,"<%=SystemEnv.getHtmlLabelName(15273,user.getLanguage())%>")){
                            WfForm.controlBtnDisabled(false);
                            return;
                        }
                        var leavetype_key = _field_fromDate_val+"_"+_field_newLeaveType_val;
                        if(resMap[_field_resourceId_val]){
                            var tmpResMap = resMap[_field_resourceId_val];
                            if(tmpResMap[leavetype_key]){
                                var tmpDay = tmpResMap[leavetype_key];
                                tmpResMap[leavetype_key] = parseFloat(tmpDay)+parseFloat(_field_duration_val);
                            }else{
                                tmpResMap[leavetype_key] = parseFloat(_field_duration_val);
                            }
                        }else{
                            daysMap = {};
                            daysMap[leavetype_key] = parseFloat(_field_duration_val);
                            resMap[_field_resourceId_val] = daysMap;
                        }
                        checkRuleData[rowIdx] = _field_resourceId_val+"_"+_field_fromDate_val+"_"+_field_fromTime_val+"_"+_field_toDate_val+"_"+_field_toTime_val;
                        checkDurationData[rowIdx] = _field_resourceId_val+"_"+currentnodetype+"_"+_field_duration_val;
                    }
                }
            }else{
                var _field_resourceId_val = WfForm.getFieldValue(_field_resourceId);
                var _field_newLeaveType_val = WfForm.getFieldValue(_field_newLeaveType);
                var _field_fromDate_val = WfForm.getFieldValue(_field_fromDate);
                var _field_fromTime_val = WfForm.getFieldValue(_field_fromTime);
                var _field_toDate_val = WfForm.getFieldValue(_field_toDate);
                var _field_toTime_val = WfForm.getFieldValue(_field_toTime);
                var _field_duration_val = WfForm.getFieldValue(_field_duration);
                if(!DateCheck(_field_fromDate_val,_field_fromTime_val,_field_toDate_val,_field_toTime_val,"<%=SystemEnv.getHtmlLabelName(15273,user.getLanguage())%>")){
                    WfForm.controlBtnDisabled(false);
                    return;
                }
                var leavetype_key = _field_fromDate_val+"_"+_field_newLeaveType_val;
                daysMap[leavetype_key] = parseFloat(_field_duration_val);
                resMap[_field_resourceId_val] = daysMap;
                checkRuleData[0] = _field_resourceId_val+"_"+_field_fromDate_val+"_"+_field_fromTime_val+"_"+_field_toDate_val+"_"+_field_toTime_val;
                checkDurationData[0] = _field_resourceId_val+"_"+currentnodetype+"_"+_field_duration_val;
            }
            var resMap2json=JSON.stringify(resMap);
            var checkRuleData2json=JSON.stringify(checkRuleData);
            var checkDurationData2json=JSON.stringify(checkDurationData);
            var _data = "resMap="+resMap2json+"&checkRuleData2json="+checkRuleData2json
                +"&workflowid="+workflowid+"&nodeid="+nodeid+"&min_duration="+min_duration+"&requestid="
                +requestid+"&currentnodetype="+currentnodetype+"&attid="+attid+"&checkDurationData2json="+checkDurationData2json;

            jQuery.ajax({
                url : "<%=weaver.general.GCONST.getContextPath()%>/api/hrm/kq/attendanceEvent/checkLeave",
                type : "post",
                processData : false,
                data : _data,
                dataType : "json",
                success: function do4Success(data){
                    if(data != null && data.status == "1"){
                        WfForm.controlBtnDisabled(false);
                        callback(); //继续提交需调用callback，不调用代表阻断
                        return;
                    }else{
                        var errorInfo = data.message;
                        if(data.status == -2){
                            if(isMobile){
                                if(data.mobile_message) {
                                    errorInfo = data.mobile_message;
                                }
                            }
                            WfForm.showConfirm(errorInfo,function(){
                                WfForm.controlBtnDisabled(false);
                                callback(); //继续提交需调用callback，不调用代表阻断
                                return;
                            },function () {
                                WfForm.controlBtnDisabled(false);
                                return;
                            });
                        }else {
                            if(isMobile){
                                var mobile_errorInfo = errorInfo;
                                if(data.mobile_message){
                                    mobile_errorInfo = data.mobile_message;
                                }
                                if(window.showMsg4kqflowMobile){
                                    window.showMsg4kqflowMobile(mobile_errorInfo);
                                }else{
                                    WfForm.showMessage(mobile_errorInfo);
                                }
                            }else{
                                if(window.showMsg4kqflow){
                                    window.showMsg4kqflow(errorInfo);
                                }else{
                                    WfForm.showMessage(errorInfo);
                                }
                            }
                            WfForm.controlBtnDisabled(false);
                            return;
                        }
                    }
                }
            });
        }catch(ex1){
            WfForm.controlBtnDisabled(false);//取消流程中的按钮置灰
            return;
        }
    }

    function null2String(s){
        if(!s){
            return "";
        }
        return s;
    }


    function DateCheck(fromDate,fromTime,toDate,toTime,msg){

        var begin = new Date(fromDate.replace(/\-/g, "\/"));
        var end = new Date(toDate.replace(/\-/g, "\/"));
        if(fromTime != "" && toTime != ""){
            begin = new Date(fromDate.replace(/\-/g, "\/")+" "+fromTime+":00");
            end = new Date(toDate.replace(/\-/g, "\/")+" "+toTime+":00");
            if(fromDate!=""&&toDate!=""&&begin >end)
            {               
			            	if(isMobile){                                                           
                                alert(msg);                                
                            }else{
                                
                                WfForm.showMessage(msg);
                                
                            }
                return false;
            }
        }else{
            if(fromDate!=""&&toDate!=""&&begin >end)
            {
			             	if(isMobile){                               
                               alert(msg);
                                
                            }else{
                                
                                WfForm.showMessage(msg);
                                
                            }
                return false;
            }
        }
        return true;
    }
</script>
