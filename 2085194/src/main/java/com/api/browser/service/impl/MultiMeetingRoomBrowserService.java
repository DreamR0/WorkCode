/*
 *
 * Copyright (c) 2001-2016 泛微软件.
 * 泛微协同商务系统,版权所有.
 *
 */
package com.api.browser.service.impl;

import com.alibaba.fastjson.JSON;
import com.api.browser.bean.*;
import com.api.browser.service.BrowserService;
import com.api.browser.util.*;
import com.api.meeting.util.FieldUtil;
import com.api.meeting.util.PageUidFactory;
import com.cloudstore.dev.api.bean.SplitMobileDataBean;
import com.cloudstore.dev.api.bean.SplitMobileTemplateBean;
import com.cloudstore.dev.api.util.Util_TableMap;
import com.engine.workflow.biz.requestForm.TestWorkflowCheckBiz;
import com.weaverboot.tools.enumTools.weaComponent.CustomBrowserEnum;
import com.weaverboot.weaComponent.impl.weaForm.impl.BrowserWeaForm;
import org.apache.commons.lang3.StringUtils;
import weaver.conn.DBUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.PageIdConst;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.meeting.MeetingShareUtil;
import weaver.meeting.MeetingUtil;
import weaver.systeminfo.SystemEnv;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 多会议室
 * type : 184
 * @author lmx 2017.06.14
 *
 */
public class MultiMeetingRoomBrowserService extends BrowserService {

    /**
     * 获取浏览框展示数据
     * @param params 查询条件等参数
     * @return 浏览框数据
     */
    @Override
    public Map<String, Object> getBrowserData(Map<String, Object> params) throws Exception {
        RecordSet rs=new RecordSet();
        Map<String, Object> apidatas = new HashMap<String, Object>();
        String meetingid = Util.null2String(params.get("meetingid"));
        String name = Util.null2String(params.get("name"));
        String roomdesc = Util.null2String(params.get("roomdesc"));
        int subcompany = Util.getIntValue((String)params.get("subcompany"),-1);
        String equipment = Util.null2String(params.get("equipment"));
        String mrtype = Util.null2String(params.get("mrtype"));
        int forall = Util.getIntValue(Util.null2String(params.get("forall")),0);
        String wfTestStr = Util.null2String(params.get("wfTestStr"));//测试流程中选择后来决定是哪个测试账号
        String beginDate = Util.null2String(params.get("beginDate"));
        String beginTime = Util.null2String(params.get("beginTime"));
        String endDate = Util.null2String(params.get("endDate"));
        String endTime = Util.null2String(params.get("endTime"));
        String attribute = Util.null2String(params.get("attribute"));
        String byWhat = Util.null2String(params.get("byWhat"));
        String roomAttribute = Util.null2String(params.get("roomAttribute"));
        String roomid = Util.null2String(params.get("roomid"));
        boolean isfromScreen = "1".equals(Util.null2String(params.get("isfromScreen")));//来自移动端大屏会议列表
        boolean isMobile = "1".equals(Util.null2String(params.get("ismobile")));//来自移动端
        String allUserdRooms = "";
        String mycity = Util.null2String(params.get("mycity"));

        new BaseBean().writeLog("====mycity====");
        new BaseBean().writeLog(mycity);
        String mybuilding = Util.null2String(params.get("mybuilding"));
        String myfloor = Util.null2String(params.get("myfloor"));
        ManageDetachComInfo mdci = new ManageDetachComInfo();
        boolean isUseMtiManageDetach = mdci.isUseMtiManageDetach();
        int detachable = 0;
        if (isUseMtiManageDetach) {
            detachable = 1;
        } else {
            detachable = 0;
        }
        //构建where语句
        String sqlwhere = "";

        sqlwhere += " 1=1";
        if(isfromScreen){//来自移动端大屏会议列表
            sqlwhere+= " and a.screenShowType>0 ";
        }

        //预留sqlwhere条件
        String sqlwhereParam = Util.null2String(params.get("sqlwhere"));
        if(!"".equals(sqlwhereParam)){
            sqlwhere += sqlwhereParam;
        }
        if (!"".equals(name)) {
            sqlwhere += " and name like '%" + name + "%'";
        }
        if (!"".equals(mycity)) {
            /*sqlwhere += " and mycity like  '%" + mycity + "%'";*/
            sqlwhere += " and mycity in (" + mycity +")";
            //正则取mycity的值
           /*List sum = new ArrayList();
           for (String num1:mycity.replaceAll("[^0-9]",",").split(",")){
               if (num1.length()>0){
                   sum.add(num1);
               }
           }
            for (int i = 0; i <sum.size() ; i++) {
                String mycitystr =(String) sum.get(i);
                if ( i  == 0){
                    sqlwhere += "and mycity like  '%" + mycitystr + "%'";
                }else {
                    sqlwhere += "or '%" + mycitystr + "%'";
                }
            }*/
        }
        if (!"".equals(mybuilding)) {
            sqlwhere += " and mybuilding like '%" + mybuilding + "%'";
        }
        if (!"".equals(myfloor)) {
            sqlwhere += " and myfloor like '%" + myfloor + "%'";
        }
        if (!"".equals(roomdesc)) {
            sqlwhere += " and roomdesc like '%" + roomdesc + "%'";
        }
        if (subcompany > 0) {
            sqlwhere += " and subcompanyid = '" + subcompany + "'";
        }
        if (!"".equals(equipment)) {
            sqlwhere += " and equipment like '%" + equipment + "%'";
        }
        if(!"".equals(mrtype)){
            sqlwhere += " and mrtype ='" + mrtype + "'";
        }
        if(!"".equals(roomAttribute)){
            sqlwhere += " and roomAttribute ='" + roomAttribute + "'";
        }
        if("real".equals(attribute)){
            sqlwhere += " and roomAttribute =0 ";
        }
        if(!"".equals(roomid)){
            sqlwhere += " and id != "+roomid;
        }
        if (forall != 1) {
            TestWorkflowCheckBiz testWorkflowCheckBiz = new TestWorkflowCheckBiz();
            String[] arr = testWorkflowCheckBiz.decodeTestStr(wfTestStr);
            User testUser = new User();
            if(arr.length == 3 && "true".equals(arr[0])) {
                int wftest_userid = Util.getIntValue(arr[1]);
                int wftest_usertype = Util.getIntValue(arr[2], 0);
                testUser = MeetingShareUtil.generateUserObj(wftest_userid, wftest_usertype);
                if("zh".equals(byWhat) ||isfromScreen){
                    sqlwhere += MeetingShareUtil.getRoomShareSqlNew(testUser);
                }else{
                    sqlwhere += MeetingShareUtil.getRoomShareSql(testUser);
                }
            }else{
                if("zh".equals(byWhat) ||isfromScreen){
                    sqlwhere += MeetingShareUtil.getRoomShareSqlNew(user);
                }else{
                    sqlwhere += MeetingShareUtil.getRoomShareSql(user);
                }
            }
            sqlwhere += " and (a.status=1 or a.status is null )";
        }
        new BaseBean().writeLog("===============sqlwhere===============");
        new BaseBean().writeLog(sqlwhere);
//		String backfields  = "a.id,a.name,a.subcompanyid,a.roomdesc,a.images,(case when b.roomstatus is null then 0 else 1 end) roomstatus";
        String backfields  = "a.id,a.name,a.subcompanyid,a.roomdesc,a.images,a.id roomstatus";
        StringBuffer sb = new StringBuffer();
        sb.append("  meetingroom a  ");
        //云商店现在没时间弄已占用的功能，所以先屏蔽下
        if(!beginDate.equals("") && !beginTime.equals("") && !endDate.equals("") && !endTime.equals("")){
            StringBuffer getAddressSql = new StringBuffer();
            getAddressSql.append("  select address,realaddress from meeting where repeatType = 0 AND meetingstatus in (1,2) ");
            getAddressSql.append(" and (endDate > ? or ( endDate = ? and endtime > ? ) )  ");
            getAddressSql.append(" and (beginDate < ? or ( beginDate = ? and begintime < ? ) )  ");
            if(!meetingid.equals("")){
                getAddressSql.append(" and id <> ?  ");
                rs.executeQuery(getAddressSql.toString(),beginDate,beginDate,beginTime,endDate,endDate,endTime,meetingid);
            }else{
                rs.executeQuery(getAddressSql.toString(),beginDate,beginDate,beginTime,endDate,endDate,endTime);
            }
//			rs.executeQuery(getAddressSql.toString(),beginDate,beginDate,beginTime,endDate,endDate,endTime);
            Set<String> roomStatusSet = new HashSet();
            while(rs.next()){
                String address = rs.getString(1);
                String realaddress = rs.getString(2);
                if(StringUtils.isNotBlank(realaddress)){
                    address = realaddress;
                }
                Set addressSet = Arrays.asList(address).stream().filter(item->!item.equals("")).collect(Collectors.toSet());
                roomStatusSet.addAll(addressSet);
            }
            allUserdRooms = roomStatusSet.stream().collect(Collectors.joining(","));
//            MeetingUtil meetingUtil = new MeetingUtil();
//            allUserdRooms = meetingUtil.dealRealMeetingAddress(allUserdRooms);
        }else{
            backfields =  "a.id,a.name,a.subcompanyid,a.roomdesc,a.images,0 roomstatus";
        }
        String fromSql = sb.toString();
        String orderby = "a.dsporder,a.name";
        List<SplitTableColBean> cols = new ArrayList<SplitTableColBean>();
        cols.add(new SplitTableColBean("true","id"));
        cols.add(new SplitTableColBean("40%",SystemEnv.getHtmlLabelName(2105, user.getLanguage()),"name","name",1).setIsInputCol(BoolAttr.TRUE).setBelong(BelongAttr.PCMOBILE).setMobileviewtype(MobileViewTypeAttr.HIGHLIGHT));
        cols.add(new SplitTableColBean("30%", SystemEnv.getHtmlLabelName(399, user.getLanguage()), "subcompanyid", "subcompanyid", "com.api.meeting.util.MeetingTransMethod.getMeetingSubCompany").setBelong(BelongAttr.PCMOBILE).setMobileviewtype(MobileViewTypeAttr.DETAIL));
        cols.add(new SplitTableColBean("60%",SystemEnv.getHtmlLabelName(780, user.getLanguage()),"roomdesc","roomdesc" ,"com.api.meeting.util.MeetingTransMethod.getRoomDescSpan","column:id+"+user.getLanguage()).setBelong(BelongAttr.PCMOBILE).setMobileviewtype(MobileViewTypeAttr.DETAIL));
        //因为要转换fieldid加密，提取出来
        SplitTableColBean imagesSplitBean = new SplitTableColBean("true","images");
        imagesSplitBean.setTransMethodForce("true");
        imagesSplitBean.setTransmethod("com.api.meeting.util.MeetingTransMethod.encodeFieldid");
        cols.add(imagesSplitBean);
        SplitTableColBean roomStatusSplitBean = new SplitTableColBean("true","roomstatus");
        if(!beginDate.equals("") && !beginTime.equals("") && !endDate.equals("") && !endTime.equals("")){
            roomStatusSplitBean.setTransMethodForce("true");
            roomStatusSplitBean.setTransmethod("com.api.meeting.util.MeetingTransMethod.getRoomUseStatus");
            roomStatusSplitBean.setOtherpara(allUserdRooms);
        }
        cols.add(roomStatusSplitBean);
        String browsertag="<browser imgurl=\"/weaver/weaver.file.FileDownload\" linkkey=\"fileid\" linkvaluecolumn=\"imagesspan\" linktitlecolumn=\"name\" desccolumn=\"roomdescspan\" subcolumn=\"subcompanyid\"/>";
        SplitTableBean tableBean  =  new SplitTableBean(backfields,fromSql,sqlwhere,orderby,"a.id","ASC",cols);

        //PC端大屏需要保存分页信息
        if(isfromScreen && !isMobile){
            String pageUid = PageUidFactory.getPageUid("multiMeetingRoomBrowser");
            String pageSize = PageIdConst.getPageSize(pageUid, user.getUID());

            tableBean.setInstanceid("multiMeetingRoomBrowser");
            tableBean.setPageID(pageUid);
            tableBean.setPageUID(pageUid);
            tableBean.setPagesize(pageSize);
        }

        //移动端展示类型
        tableBean.setMobileshowtype(MobileShowTypeAttr.ListView);
        try {
//			tableBean.createMobileTemplate(MobileJsonConfigUtil.getSplitMobileTemplateBean(getJonsConfig()));
            tableBean.createMobileTemplate(getMobileTemp());
        } catch (Exception e) {
            e.printStackTrace();
        }

        apidatas.putAll(makeListDataResult(tableBean,browsertag));
        return apidatas;
    }

    /**
     * 获取手机展示模板
     * @return
     */
    private List getJonsConfig(){
        List<SplitMobileDataBean> list=new ArrayList<SplitMobileDataBean>();
        Map style=new HashMap();
        style.put("flex","none");

        MobileJsonConfigUtil.addKey(list,"col1",style);
        //对images 特殊处理
        Map imgMap=new HashMap();
        imgMap.put("key","imagesspan");
        imgMap.put("isimg",true);
        Map imgUrlMap=new HashMap();
        imgUrlMap.put("url","/weaver/weaver.file.FileDownload?fileid=");
        imgUrlMap.put("defaultUrl","/cloudstore/resource/pc/com/images/meeting_default.png");
        imgMap.put("ismeetingroom",imgUrlMap);
        style=new HashMap();
        style.put("width","127px");
        style.put("height","85px");
        imgMap.put("style",style);
        //添加style
        List configs=new ArrayList();
        configs.add(imgMap);

        MobileJsonConfigUtil.addKey(list,"col1.row1",configs);
        MobileJsonConfigUtil.addKey(list,"col2.row1.name",null,null,null,true);
        MobileJsonConfigUtil.addKey(list,"col2.row2.subcompanyid");
        MobileJsonConfigUtil.addKey(list,"col2.row3.roomdesc");
        MobileJsonConfigUtil.addKey(list,"col2.row4.roomstatus");

        return list;
    }

    /**
     * 设置浏览框高级查询条件
     * @param params 参数
     * @return 查询条件数据
     */
    @Override
    public Map<String, Object> getBrowserConditionInfo(Map<String, Object> params) throws Exception {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        String byWhat = Util.null2String(params.get("byWhat"));
        List<SearchConditionOption> selOptions  = new ArrayList<SearchConditionOption>();
        selOptions.add(new SearchConditionOption("",""));
        RecordSet rs=new RecordSet();
        rs.execute("select * from MeetingRoom_type order by dsporder");
        while(rs.next()){
            selOptions.add(new SearchConditionOption(rs.getString("id"),rs.getString("name")));
        }

        List/*<SearchConditionItem>*/ conditions = new ArrayList/*<SearchConditionItem>*/();
        ConditionFactory conditionFactory = new ConditionFactory(user);
        conditions.add(conditionFactory.createCondition(ConditionType.INPUT, 31232, "name",true));
        conditions.add(conditionFactory.createCondition(ConditionType.INPUT, 433, "roomdesc"));
        ManageDetachComInfo mdci = new ManageDetachComInfo();/**/
        boolean isUseMtiManageDetach = mdci.isUseMtiManageDetach();
        if(isUseMtiManageDetach){
            conditions.add(conditionFactory.createCondition(ConditionType.BROWSER, 17868, "subcompany","164"));
        }
        conditions.add(conditionFactory.createCondition(ConditionType.SELECT, "780,63", "mrtype",selOptions));
        if(!"zh".equals(byWhat)){
            conditions.add(conditionFactory.createCondition(ConditionType.SELECT, "528752", "roomAttribute",getRoomAttributeOption(user.getLanguage())));
        }

        //==zj==控制“所在城市”查询条件
        BrowserWeaForm citysci = new BrowserWeaForm("所在城市","customCity", CustomBrowserEnum.MULTIPLE,7,"mycity");
        BrowserBean bcp = citysci.getBrowserConditionParam();
        bcp.setCompleteURL("/api/public/browser/complete/");
        bcp.setConditionURL("/api/public/browser/condition/");
        new BaseBean().writeLog("=====getBrowserConditionInfo(params)====");
        new BaseBean().writeLog(Util.null2String(params.get("ismobile")));
        if (Util.null2String(params.get("ismobile")).equals("1"))
        {
            bcp.setDataURL("/api/public/browser/data/");
        }
        citysci.setBrowserConditionParam(bcp);

        new BaseBean().writeLog("=====设置浏览框高级查询条件(citysci)====");
        new BaseBean().writeLog(JSON.toJSONString(citysci));
        conditions.add(citysci);
        conditions.add(conditionFactory.createCondition(ConditionType.INPUT, 528827, "mybuilding"));
        conditions.add(conditionFactory.createCondition(ConditionType.INPUT, 528826, "myfloor"));
        apidatas.put(BrowserConstant.BROWSER_RESULT_CONDITIONS, conditions);

        return apidatas;
    }

    public List getRoomAttributeOption(int languageid) {
        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
        options.add(new SearchConditionOption("",SystemEnv.getHtmlLabelName(332, languageid), true));
        options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(	528753, languageid)));
        options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(	528754, languageid)));
        options.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(	528755, languageid)));


        new BaseBean().writeLog("===========getRoomAttributeOption===========");
        new BaseBean().writeLog(JSON.toJSONString(options));
        return options;
    }
    /**
     * 绑定sessionkey
     * @param splitTable
     * @param browsertag
     * @return
     */
    public Map<String,Object> makeListDataResult(SplitTableBean splitTable,String browsertag) {
        Map<String,Object> result  = new HashMap<String,Object>();
        String sessionkey = Util.getEncrypt(Util.getRandom());
        Util_TableMap.setVal(sessionkey, getTableString(splitTable,browsertag));
        result.put(BrowserConstant.BROWSER_RESULT_TYPE, BrowserDataType.LIST_SPLIT_DATA.getTypeid());
        result.put(BrowserConstant.BROWSER_RESULT_DATA, sessionkey);
        return result;
    }

    /**
     * 拼接表单字符串
     * @param splitTable
     * @param browsertag
     * @return
     */
    public String getTableString(SplitTableBean splitTable,String browsertag) {
        String SUF_MARK = "\"";
        StringBuilder tableString = new StringBuilder();
        // table
        tableString.append("<table tabletype=\"").append(splitTable.getTableType()).append(SUF_MARK);
        if (splitTable.getPageUID() != null) {
            tableString.append(" pageUid=\"").append(splitTable.getPageUID()).append(SUF_MARK);
        }
        tableString.append(" pagesize=\"").append(splitTable.getPagesize()).append(SUF_MARK);
        if (splitTable.getDatasource() != null) {
            tableString.append(" datasource=\"").append(splitTable.getDatasource()).append(SUF_MARK);
        }
        if (splitTable.getSourceparams() != null) {
            tableString.append(" sourceparams=\"").append(splitTable.getSourceparams()).append(SUF_MARK);
        }
        //移动端属性
        tableString.append(" mobileshowtype=\"").append(splitTable.getMobileshowtype().getStringVal()).append(SUF_MARK);
        if(splitTable.getMobileshowtemplate() != null) {
            tableString.append(" mobileshowtemplate=\"").append(splitTable.getMobileshowtemplate()).append(SUF_MARK);
        }
        tableString.append(">");
        // sql
        tableString.append("<sql backfields=\"").append(splitTable.getBackfields()).append(SUF_MARK);
        tableString.append(" sqlform=\"").append(Util.toHtmlForSplitPage(splitTable.getSqlform())).append(SUF_MARK);
        tableString.append(" sqlwhere=\"").append(Util.toHtmlForSplitPage(splitTable.getSqlwhere())).append(SUF_MARK);
        if(!"".equals(Util.null2String(splitTable.getSqlorderby()))){
            tableString.append(" sqlorderby=\"").append(splitTable.getSqlorderby()).append(SUF_MARK);
            tableString.append(" sqlsortway=\"").append(splitTable.getSqlsortway()).append(SUF_MARK);
        }
        tableString.append(" sqlprimarykey=\"").append(splitTable.getSqlprimarykey()).append(SUF_MARK);
        tableString.append(" sqlisdistinct=\"").append(splitTable.getSqlisdistinct()).append(SUF_MARK);
        tableString.append("/>");
        tableString.append(browsertag);
        String primarykey = Util.null2String(splitTable.getSqlprimarykey());
        if(primarykey.indexOf(".") > 0){
            primarykey = primarykey.substring(primarykey.indexOf(".") + 1);
        }
        // col
        tableString.append("<head>");
        List<SplitTableColBean> cols = splitTable.getCols();
        for (SplitTableColBean colBean : cols) {
            tableString.append("<col hide=\"").append(colBean.getHide()).append(SUF_MARK);
            if (colBean.getWidth() != null) {
                tableString.append(" width=\"").append(colBean.getWidth()).append(SUF_MARK);
            }
            if (colBean.getText() != null) {
                tableString.append(" text=\"").append(colBean.getText()).append(SUF_MARK);
            }
            if (colBean.getColumn() != null) {
                tableString.append(" column=\"").append(colBean.getColumn()).append(SUF_MARK);
            }
            if (colBean.getOrderkey() != null) {
                tableString.append(" orderkey=\"").append(colBean.getOrderkey()).append(SUF_MARK);
            }
            tableString.append(" belong=\"").append(colBean.getBelong().getStringVal()).append(SUF_MARK);

            if (colBean.getBelong() != BelongAttr.PC)
                tableString.append(" mobileviewtype=\"").append(colBean.getMobileviewtype().getStringVal()).append(SUF_MARK);

            if (colBean.getTransmethod() != null) {
                tableString.append(" transmethod=\"").append(colBean.getTransmethod()).append(SUF_MARK);
                tableString.append(" display=\"true\" ");
            }
            if (colBean.getOtherpara() != null) {
                tableString.append(" otherpara=\"").append(colBean.getOtherpara()).append(SUF_MARK);
            }
            if(colBean.getTransMethodForce() != null){
                tableString.append(" transMethodForce=\"").append(colBean.getTransMethodForce()).append(SUF_MARK);
            }
            tableString.append(" showType=\"").append(colBean.getShowType()).append(SUF_MARK);
            tableString.append(" isInputCol=\"").append(colBean.getIsInputCol().toString()).append(SUF_MARK);
            BoolAttr isPrimarykey  = BoolAttr.TRUE == colBean.getIsPrimarykey() ? colBean.getIsPrimarykey() : (Util.null2String(colBean.getColumn()).equalsIgnoreCase(primarykey) ? BoolAttr.TRUE:BoolAttr.FALSE);
            tableString.append(" isPrimarykey=\"").append(isPrimarykey.toString()).append(SUF_MARK);

            tableString.append("/>");
        }
        tableString.append("</head>");
        tableString.append("</table>");
        return tableString.toString();
    }

    public SplitMobileTemplateBean getMobileTemp(){
        SplitMobileTemplateBean  splitMobileTemplateBean = new SplitMobileTemplateBean();
        Map map = new HashMap<String, Object>();
        List meetingColumnList = new ArrayList();
        meetingColumnList.add("imagesspan");
        meetingColumnList.add("name");
        meetingColumnList.add("subcompanyid");
        meetingColumnList.add("roomdesc");
        meetingColumnList.add("roomstatusspan");
        map.put("dataKeys",meetingColumnList);
        map.put("theme","meeting");
        splitMobileTemplateBean.put("meeting",map);
        return splitMobileTemplateBean;
    }

    @Override
    public Map<String, Object> getMultBrowserDestData(Map<String, Object> params) throws Exception {
        String selectids=Util.null2String(params.get("selectids"));
        if(!"".equals(selectids)){
            RecordSet rs=new RecordSet();
            if(selectids.startsWith(",")) selectids=selectids.substring(1);
            if(selectids.endsWith(",")) selectids= selectids.substring(0,selectids.length()-1);
            List idParams = new ArrayList();
            rs.executeQuery("select a.id,a.name,a.subcompanyid,a.roomdesc,a.images from meetingRoom a where id in ("+ DBUtil.getParamReplace(selectids)+") order by a.dsporder,a.name",DBUtil.trasToList(idParams,selectids));
            List browser=new ArrayList();
            List datas=new ArrayList();
            Map browserMap;
            Map dataMap;
            SubCompanyComInfo subCompanyComInfo=new SubCompanyComInfo();
            while (rs.next()){
                browserMap=new HashMap();
                dataMap=new HashMap();
                browserMap.put("desccolumn",rs.getString("roomdesc"));
                browserMap.put("imgurl","/weaver/weaver.file.FileDownload");
                browserMap.put("linkkey","fileid");
                browserMap.put("linktitlecolumn",rs.getString("name"));
                browserMap.put("linkvaluecolumn",rs.getString("images"));
                browserMap.put("list",rs.getString("roomdesc"));
                browserMap.put("subcolumn",subCompanyComInfo.getSubCompanyname(rs.getString("subcompanyid")));

                browser.add(browserMap);

                dataMap.put("id",rs.getString("id"));
                dataMap.put("idspan",dataMap.get("id"));
                dataMap.put("images",rs.getString("images"));
                dataMap.put("imagesspan",dataMap.get("images"));
                dataMap.put("name",rs.getString("name"));
                dataMap.put("namespan",dataMap.get("name"));
                dataMap.put("randomFieldId",rs.getString("id"));
                dataMap.put("randomFieldIdspan","");
                dataMap.put("roomdesc",rs.getString("roomdesc"));
                dataMap.put("roomdescspan",dataMap.get("roomdesc"));
                dataMap.put("subcompanyid",rs.getString("subcompanyid"));
                dataMap.put("subcompanyidspan",subCompanyComInfo.getSubCompanyname(rs.getString("subcompanyid")));

                datas.add(dataMap);
            }

            Map map=new HashMap<String, Object>();
            map.put("browser",browser);
            map.put("datas",datas);

            if("1".equals(Util.null2String(params.get("ismobile")))){
                List<SplitTableColBean> cols = new ArrayList<SplitTableColBean>();
                cols.add(new SplitTableColBean("true","id"));
                cols.add(new SplitTableColBean("40%",SystemEnv.getHtmlLabelName(2105, user.getLanguage()),"name","name",1).setIsInputCol(BoolAttr.TRUE).setBelong(BelongAttr.PCMOBILE).setMobileviewtype(MobileViewTypeAttr.HIGHLIGHT));
                cols.add(new SplitTableColBean("30%", SystemEnv.getHtmlLabelName(399, user.getLanguage()), "subcompanyid", "subcompanyid", "com.api.meeting.util.MeetingTransMethod.getMeetingSubCompany").setBelong(BelongAttr.PCMOBILE).setMobileviewtype(MobileViewTypeAttr.DETAIL));
                cols.add(new SplitTableColBean("60%",SystemEnv.getHtmlLabelName(780, user.getLanguage()),"roomdesc","roomdesc").setBelong(BelongAttr.PCMOBILE).setMobileviewtype(MobileViewTypeAttr.DETAIL));
                cols.add(new SplitTableColBean("true","images"));

                map.put(BrowserConstant.BROWSER_RESULT_COLUMN, cols);
                map.put(BrowserConstant.BROWSER_RESULT_TYPE, BrowserDataType.LIST_ALL_DATA.getTypeid());
            }

            return map;

        }
        return new HashMap<String, Object>();
    }
}
