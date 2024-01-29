package com.engine.meeting.cmd.meetingroom;

import com.alibaba.fastjson.JSON;
import com.api.browser.bean.BrowserBean;
import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.browser.util.ConditionFactory;
import com.api.browser.util.ConditionType;
import com.api.meeting.util.FieldUtil;
import com.api.meeting.util.MeetingSecIdUtil;
import com.engine.common.biz.AbstractBizLog;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.Command;
import com.engine.core.interceptor.CommandContext;
import com.engine.meeting.util.FunctionSwitch;
import com.engine.meeting.util.MeetingNoDataUtil;
import com.engine.meeting.util.MeetingNoRightUtil;
import com.engine.meeting.util.MeetingSelectOptionsUtil;
import com.weaverboot.tools.enumTools.weaComponent.CustomBrowserEnum;
import com.weaverboot.weaComponent.impl.weaForm.impl.BrowserWeaForm;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.meeting.MeetingShareUtil;
import weaver.meeting.MeetingUtil;
import weaver.meeting.util.exchange.MeetingExchangeUtil;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.systemright.CheckSubCompanyRight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class GetRoomFieldsCmd extends AbstractCommonCommand<Map<String, Object>> {

	public GetRoomFieldsCmd(User user, Map<String, Object> params){
		this.user = user;
        this.params = params;
	}
	
	@Override
	public BizLogContext getLogContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		//判断分权
		ManageDetachComInfo manageDetachComInfo=new ManageDetachComInfo();
		//是否开启会议分权
		String detachable= manageDetachComInfo.getMtidetachable();
		Map resMap=new HashMap();
		int languageid = user.getLanguage();
		String roomid = Util.null2String(params.get("roomid"));//会议室id
		String prview = Util.null2String(params.get("preview"));//0新建,编辑  1查看
		int subCompanyId = Util.getIntValue(Util.null2String(params.get("subCompanyId")));//左侧选中树
		int fromN = Util.getIntValue(Util.null2String(params.get("fromN")),0);//是否来自新建会议页面 1 是
		String name = "";
		String type ="";
		String subid = "";
		String hrmids = "";
		String status = "";
		String desc = "";
		String ewsemail = "";
		String dsporder = "";
		String equipment = "";
		String images ="";
		String screenShowType = "";
		//自定义城市名称
		String csm="";
		int beforeShowMeetingTime = 30;
		int isShowQRCode = 0;
		int viewAttr = 2;
		int arrangeSeat = 0;//启用排座
		boolean showArrangeSeat=true;//是否展示  启用排座按钮
		int roomAttribute = 0;
		String mycity = "0";
		int vrmCheck = 0;
		String slaverRooms = "";
		String mybuilding = "";
		String myfloor = "";
		int allowMinNum = 0;
		int allowMaxNum = 0;
		int minNumChk = 0;
		int maxNumChk = 0;
		int minNumChkType = 0;
		int maxNumChkType = 0;
		int canOrderDay = 0;
		int maxOrderDay = 0;
		int onlyWorkDay = 0;
		//传入此参数. 永远是只读.不能编辑操作.
		if("1".equals(prview)){
			viewAttr=1;//只读
			resMap.put("readOnly",true);
			RecordSet RecordSet=new RecordSet();
			//判断是否存在该会议室
			RecordSet.executeQuery("select id from MeetingRoom where id = ?",roomid);
			if(!RecordSet.next()){
				return MeetingNoDataUtil.getNoDataMap();
			}
		}else{//新建编辑
			if(!HrmUserVarify.checkUserRight("MeetingRoomAdd:Add",user)) {
				return MeetingNoRightUtil.getNoRightMap();
			}
		}

		RecordSet rs = new RecordSet();
		//查询自定义城市表单
		RecordSet rc = new RecordSet();
		if(!roomid.isEmpty()){//查看,或者编辑
			rs.executeQuery("select * from MeetingRoom where id = ?" , roomid);
		if(rs.next()){
				hrmids = Util.null2String(rs.getString("hrmids"));
				subid = rs.getString("subcompanyid");
				name = Util.null2String(rs.getString("name"));
				status = Util.null2String(rs.getString("status"));
				status = (status == null || status.equals("")) ? "1": status;
				desc = Util.null2String(rs.getString("roomdesc"));
				ewsemail = Util.null2String(rs.getString("ewsemail"));
				dsporder = Util.getPointValue3(rs.getString("dsporder"), 1, "0");
				equipment = Util.null2String(rs.getString("equipment"));
				images = Util.null2String(rs.getString("images"));
				type = Util.null2String(rs.getString("mrtype"));
				screenShowType =Util.null2String(rs.getString("screenShowType"));
				arrangeSeat =Util.getIntValue(rs.getString("arrangeSeat"),0);//启用排座
				beforeShowMeetingTime =Util.getIntValue(rs.getString("beforeShowMeetingTime"),30);
				isShowQRCode =Util.getIntValue(rs.getString("isShowQRCode"),0);
				roomAttribute =Util.getIntValue(rs.getString("roomAttribute"),0);
				vrmCheck =Util.getIntValue(rs.getString("vrmCheck"),0);
				mycity =rs.getString("mycity");
				slaverRooms =Util.null2String(rs.getString("slaverRooms"));
				mybuilding =Util.null2String(rs.getString("mybuilding"));
				myfloor =Util.null2String(rs.getString("myfloor"));
				allowMinNum =Util.getIntValue(rs.getString("allowMinNum"),0);
				allowMaxNum =Util.getIntValue(rs.getString("allowMaxNum"),0);
				minNumChk =Util.getIntValue(rs.getString("minNumChk"),0);
				maxNumChk =Util.getIntValue(rs.getString("maxNumChk"),0);
				minNumChkType =Util.getIntValue(rs.getString("minNumChkType"),0);
				maxNumChkType =Util.getIntValue(rs.getString("maxNumChkType"),0);
				canOrderDay =Util.getIntValue(rs.getString("canOrderDay"),0);
				maxOrderDay =Util.getIntValue(rs.getString("maxOrderDay"),0);
				onlyWorkDay =Util.getIntValue(rs.getString("onlyWorkDay"),0);
				//showArrangeSeat=true;
			}else{
				//无数据.
				return MeetingNoRightUtil.getNoRightMap();
			}
		//==zj==执行自定义城市表单查询
			new BaseBean().writeLog("mycity(获取)");
			new BaseBean().writeLog(mycity);
			String tbname = Util.null2String(new BaseBean().getPropValue("qc2085194","tablename"));
			new BaseBean().writeLog("==zj==配置表名获取 tbname:"+tbname);
			String citySql = "select * from " +  tbname + " where id = "+mycity;
			new BaseBean().writeLog("==zj== citySql:" + citySql);
			rc.executeQuery(citySql);
		if (rc.next()){
			csm = Util.null2String(rc.getString("csm"));
		}
		new BaseBean().writeLog("csm(获取)");
		new BaseBean().writeLog(csm);
			//开启分权,没有机构维护.直接返回只读,查看权限
			if("1".equals(detachable)){
				CheckSubCompanyRight checkSubCompanyRight=new CheckSubCompanyRight();
				int operatelevel = checkSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(),"MeetingRoomAdd:Add", Util.getIntValue(subid));
				if (!"0".equals(subid) && operatelevel < 1) {
					viewAttr=1;//只读
					resMap.put("readOnly",true);
				} else {
					if (operatelevel == 0) {
						viewAttr = 1;//只读
						resMap.put("readOnly", true);
					}
				}
			}else{
				//没有编辑权限.只读
				if (!HrmUserVarify.checkUserRight("MeetingRoomAdd:Add", user)) {
					viewAttr=1;//只读
					resMap.put("readOnly",true);
				}
			}

		}else{
			//新建,是否有编辑
			if (!HrmUserVarify.checkUserRight("MeetingRoomAdd:Add", user)) {
				return MeetingNoRightUtil.getNoRightMap();
			}

			//判断所属机构或者自身分部
			if (subCompanyId < 0) {
				subCompanyId = user.getUserSubCompany1();
			}
			subid=subCompanyId+"";
			//开启分权
			if("1".equals(detachable)){
				CheckSubCompanyRight checkSubCompanyRight=new CheckSubCompanyRight();
				int operatelevel = checkSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(),"MeetingRoomAdd:Add", subCompanyId);
				if (subCompanyId != 0 && operatelevel < 1) {
					subid="";
				}
			}else{
				if(subCompanyId==0){
					subid="";
				}
			}
			//在应用分权开启的情况下,系统管理员会出现subid=0的情况,这样在不填写的时候校验也会通过
			if(Util.getIntValue(subid,0) < 1){
				subid = "";
			}
		}

		MeetingSelectOptionsUtil meetingSelectOptionsUtil = new MeetingSelectOptionsUtil();
		ConditionFactory conditionFactory = new ConditionFactory(user);
		List<Map<String,Object>> grouplist = new ArrayList<Map<String,Object>>();
		Map<String,Object> groupitem = new HashMap<String,Object>();
		Map<String, Object> hideFieldMap = new HashMap<String, Object>();
		List hideColumnlist = new ArrayList();
		List itemlist = new ArrayList();
		itemlist.add(FieldUtil.getFormItemForMultiInput("roomname", SystemEnv.getHtmlLabelName(31232, Util.getIntValue(user.getLanguage())), name, viewAttr==1?1:3));
		//会议室属性
        itemlist.add(FieldUtil.getFormItemForSelect("roomAttribute", SystemEnv.getHtmlLabelName(528752, Util.getIntValue(languageid)), roomAttribute+"", viewAttr, this.getRoomAttributeOption(user.getLanguage())));
		//是否开启冲突检测
		itemlist.add(FieldUtil.getFormItemForSwitch("vrmCheck", SystemEnv.getHtmlLabelName(528762, languageid), String.valueOf(vrmCheck), viewAttr));//启用座位布局
		//会议室浏览按钮
		SearchConditionItem roomsci = null;
		roomsci = conditionFactory.createCondition(ConditionType.BROWSER,	780 , "slaverRooms", "184",viewAttr);
		roomsci.getBrowserConditionParam().getDataParams().put("attribute","real");
		roomsci.getBrowserConditionParam().getDataParams().put("byWhat","zh");//放个标识，设置组合会议室
		roomsci.getBrowserConditionParam().getDataParams().put("roomid",roomid);
		roomsci.getBrowserConditionParam().getCompleteParams().put("attribute","real");
		roomsci.getBrowserConditionParam().getCompleteParams().put("byWhat","zh");
		roomsci.getBrowserConditionParam().getCompleteParams().put("roomid",roomid);
		roomsci.getBrowserConditionParam().getConditionDataParams().put("byWhat","zh");
		itemlist.add(FieldUtil.getFormItemForBrowser(roomsci, "slaverRooms", SystemEnv.getHtmlLabelName(	780, languageid), "184", slaverRooms,viewAttr));
		//类型
		itemlist.add(FieldUtil.getFormItemForSelect("mrtype", SystemEnv.getHtmlLabelName(383904, Util.getIntValue(languageid)), type, viewAttr, meetingSelectOptionsUtil.getRoomTypeOption(false,languageid)));
		//所属机构
		SearchConditionItem subsci = null;
		//根据分权显示不同机构浏览框
		if("1".equals(detachable)){
			//所属机构
			subsci = conditionFactory.createCondition(ConditionType.BROWSER,17868 , "subcompanyid", "169");
			subsci.getBrowserConditionParam().setViewAttr(viewAttr==1?1:3);
			subsci.getBrowserConditionParam().getDataParams().put("rightStr","MeetingRoomAdd:Add");
			subsci.getBrowserConditionParam().getCompleteParams().put("rightStr", "MeetingRoomAdd:Add");
			subsci.getBrowserConditionParam().getConditionDataParams().put("rightStr", "MeetingRoomAdd:Add");
			itemlist.add(FieldUtil.getFormItemForBrowser(subsci, "subCompanyId", SystemEnv.getHtmlLabelName(17868, languageid), "169", subid));
		}else{
			subsci = conditionFactory.createCondition(ConditionType.BROWSER,17868 , "subcompanyid", "164");
			subsci.getBrowserConditionParam().setViewAttr(viewAttr==1?1:3);
			itemlist.add(FieldUtil.getFormItemForBrowser(subsci, "subCompanyId", SystemEnv.getHtmlLabelName(17868, languageid), "164", subid));
		}

		//负责人
		SearchConditionItem hrmsci = conditionFactory.createCondition(ConditionType.BROWSER, 2097, "hrmids", "17");
		if(!hrmids.isEmpty()){
			List listValue = new ArrayList();
			String[] arr=hrmids.split(",");
			ResourceComInfo rci = null;
			try {
				rci = new ResourceComInfo();
				for(int i=0;i<arr.length;i++){
					if(arr[i].isEmpty()){
						continue;
					}
					String showname=rci.getLastname(arr[i]);
					
					Map tmp =new HashMap();
					if(showname.isEmpty()&&!"0".equals(arr[i])&&!"-1".equals(arr[i])){//显示名为空，取id值
						showname=arr[i];
					}
					if(!showname.isEmpty()){//显示名和id都不为空才组装返回值
						tmp.put("id", arr[i]);
						tmp.put("name", showname);
					}
					listValue.add(tmp);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			hrmsci.getBrowserConditionParam().setReplaceDatas(listValue);
		}
		hrmsci.getBrowserConditionParam().setViewAttr(viewAttr);
		itemlist.add(FieldUtil.getFormItemForBrowser(hrmsci, "hrmids", SystemEnv.getHtmlLabelName(2097, languageid), "17", hrmids));
		//状态
		itemlist.add(FieldUtil.getFormItemForSelect("status", SystemEnv.getHtmlLabelName(25005, languageid), status, viewAttr, meetingSelectOptionsUtil.getRoomStatusOption(languageid,false)));
		//会议室描述
		itemlist.add(FieldUtil.getFormItemForInput("roomdesc", SystemEnv.getHtmlLabelName(10000170, Util.getIntValue(languageid)), desc, viewAttr));
		MeetingExchangeUtil meetingExchangeUtil = new MeetingExchangeUtil();
		if(meetingExchangeUtil.canUseExchangeNew()){
			itemlist.add(FieldUtil.getFormItemForInput("ewsemail", SystemEnv.getHtmlLabelName(518543, languageid), ewsemail, viewAttr));
		}
		//排序
		Map<String, Object> itemMap=FieldUtil.getFormItemForInputNumber("dsporder", SystemEnv.getHtmlLabelName(15513, languageid), dsporder,-999,999, viewAttr);
		itemMap.put("step",0.1);
		itemMap.put("precision",1);
		itemlist.add(itemMap);

		//会议排座
		if(!FunctionSwitch.canUse("MEETING_ADDRESS_SEATS")){//未开启排座功能
			showArrangeSeat=false;
		}
		if(showArrangeSeat){
			if(arrangeSeat!=1&&arrangeSeat!=0){
				arrangeSeat=0;
			}
			itemlist.add(FieldUtil.getFormItemForSwitch("arrangeSeat", SystemEnv.getHtmlLabelName(506588, languageid), String.valueOf(arrangeSeat), viewAttr));//启用座位布局
		}

		//s设备
		itemMap=FieldUtil.getFormItemForTagGroup(new String[]{"equipment"}, SystemEnv.getHtmlLabelName(500749, Util.getIntValue(languageid)), viewAttr,equipment);
		itemMap.put("noSearch",true);
		itemlist.add(itemMap);

		//大屏设置
		SearchConditionItem screenSet = conditionFactory.createCondition(ConditionType.BROWSER, 502923, "screenShowType", "meetingRoomScreen");
		screenSet.setViewAttr(viewAttr);
		screenSet.getBrowserConditionParam().setNoOperate(false);
		screenSet.getBrowserConditionParam().setViewAttr(viewAttr);
		if(!screenShowType.equals("")){
			rs.executeQuery("select name from meetingRoomScreen_Set where id = ?",screenShowType);
			List listValue = new ArrayList();
			while(rs.next()){
				Map tmp =new HashMap();
				tmp.put("id",screenShowType);
				tmp.put("name",rs.getString("name"));
				listValue.add(tmp);
			}
			screenSet.getBrowserConditionParam().setReplaceDatas(listValue);
		}

		itemlist.add(screenSet);

		itemlist.add(FieldUtil.getFormItemForInputNumber("beforeShowMeetingTime", "", beforeShowMeetingTime+"" , 0, 99999, viewAttr));
		hideColumnlist.add("beforeShowMeetingTime");
		hideFieldMap.put("beforeShowMeetingTime", FieldUtil.getFormItemForInputNumber("beforeShowMeetingTime", "", beforeShowMeetingTime+"" , 0, 99999, viewAttr));
		itemlist.add(FieldUtil.getFormItemForSwitch("isShowQRCode", SystemEnv.getHtmlLabelName(513909 , user.getLanguage()), isShowQRCode+"" , viewAttr));

		//图片上传
		String secId = "0";//MeetingSecIdUtil.getSecId(new MeetingSetInfo().getMtngAttchCtgry());
		String maxsize="0";//MeetingSecIdUtil.getMaxsize(secId);
		Map<String, Object> uploadItem=FieldUtil.getFormItemForUpload("images", SystemEnv.getHtmlLabelName(10000858, Util.getIntValue(user.getLanguage())), secId, Util.getIntValue(maxsize), "jpg,png,gif","img", viewAttr, images);
		Map cfg = new MeetingUtil().getAccessoryBaseInfo(user,"","","");
		uploadItem.put("category",cfg.containsKey("category")?cfg.get("category"):"");
		uploadItem.put("maxUploadSize", cfg.containsKey("maxUploadSize")?cfg.get("maxUploadSize"):"");
		uploadItem.put("mixUploadSize",cfg.containsKey("mixUploadSize")?cfg.get("mixUploadSize"):"" );
		if(viewAttr != 1){
			uploadItem.put("errorMsg", cfg.containsKey("errorMsg")?cfg.get("errorMsg"):"");
		}
		uploadItem.put("autoUpload",true);
		uploadItem.put("listType","img");
		itemlist.add(uploadItem);

		Map allowMinNumShow = new HashMap();
		allowMinNumShow = FieldUtil.getFormItemForInputNumber("allowMinNum", SystemEnv.getHtmlLabelName(	30138, Util.getIntValue(user.getLanguage())), "" + allowMinNum, 0, 99999, viewAttr);
		itemlist.add(allowMinNumShow);

		Map allowMaxNumShow = new HashMap();
		allowMaxNumShow = FieldUtil.getFormItemForInputNumber("allowMaxNum", SystemEnv.getHtmlLabelName(	30138, Util.getIntValue(user.getLanguage())), "" + allowMaxNum, 0, 99999, viewAttr);
		itemlist.add(allowMaxNumShow);

		hideColumnlist.add("allowMaxNum");
		hideFieldMap.put("allowMaxNum", FieldUtil.getFormItemForInputNumber("allowMaxNum", SystemEnv.getHtmlLabelName(	30138, Util.getIntValue(user.getLanguage())), "" + allowMaxNum, 0, 99999, viewAttr));

		Map minNumChkShow = new HashMap();
        minNumChkShow = FieldUtil.getFormItemForSwitch("minNumChk", SystemEnv.getHtmlLabelName(	528991, Util.getIntValue(user.getLanguage())), "" + minNumChk, viewAttr);
		itemlist.add(minNumChkShow);

		Map minNumChkTypeMap = new HashMap();
		minNumChkTypeMap = FieldUtil.getFormItemForSelect("minNumChkType", SystemEnv.getHtmlLabelName(528991, Util.getIntValue(languageid)), minNumChkType+"", viewAttr, this.getNumChkOption(user.getLanguage()));
		itemlist.add(minNumChkTypeMap);

		hideColumnlist.add("minNumChkType");
		hideFieldMap.put("minNumChkType", FieldUtil.getFormItemForSelect("minNumChkType", SystemEnv.getHtmlLabelName(528991, Util.getIntValue(languageid)), minNumChkType+"", viewAttr, this.getNumChkOption(user.getLanguage())));

        itemlist.add(FieldUtil.getFormItemForSwitch("maxNumChk", SystemEnv.getHtmlLabelName(528994, Util.getIntValue(user.getLanguage())), "" + maxNumChk, viewAttr));
		itemlist.add(FieldUtil.getFormItemForSelect("maxNumChkType", SystemEnv.getHtmlLabelName(528994, Util.getIntValue(languageid)), maxNumChkType+"", viewAttr, this.getNumChkOption(user.getLanguage())));

		hideColumnlist.add("maxNumChkType");
		hideFieldMap.put("maxNumChkType", FieldUtil.getFormItemForSelect("maxNumChkType", SystemEnv.getHtmlLabelName(528994, Util.getIntValue(languageid)), maxNumChkType+"", viewAttr, this.getNumChkOption(user.getLanguage())));

		Map canOrderDayShow = new HashMap();
		canOrderDayShow = FieldUtil.getFormItemForInputNumber("canOrderDay", SystemEnv.getHtmlLabelName(529016, Util.getIntValue(user.getLanguage())), "" + canOrderDay, 0, 99999, viewAttr);
		itemlist.add(canOrderDayShow);

		itemlist.add(FieldUtil.getFormItemForCheckbox("onlyWorkDay", SystemEnv.getHtmlLabelName(528994, Util.getIntValue(user.getLanguage())), "" + onlyWorkDay, viewAttr));

		hideColumnlist.add("onlyWorkDay");
		hideFieldMap.put("onlyWorkDay", FieldUtil.getFormItemForCheckbox("onlyWorkDay", SystemEnv.getHtmlLabelName(528994, Util.getIntValue(user.getLanguage())), "" + onlyWorkDay, viewAttr));

		Map maxOrderDayShow = new HashMap();
		maxOrderDayShow = FieldUtil.getFormItemForInputNumber("maxOrderDay", SystemEnv.getHtmlLabelName(529018, Util.getIntValue(user.getLanguage())), "" + maxOrderDay, 0, 99999, viewAttr);
		itemlist.add(maxOrderDayShow);

		groupitem.put("title", SystemEnv.getHtmlLabelName(1361, user.getLanguage()));
		groupitem.put("defaultshow", true);
		groupitem.put("items", itemlist);
		grouplist.add(groupitem);

		groupitem = new HashMap<String, Object>();
		itemlist = new ArrayList();
		/*SearchConditionItem citysci = null;
		citysci = conditionFactory.createCondition(ConditionType.BROWSER,17868 , "mycity", "58");
*/

		//==zj==更改会议信息会议室所在城市
		BrowserWeaForm citysci = new BrowserWeaForm("所在城市","customCity", CustomBrowserEnum.RADIO,7,"mycity");
		List<Map<String,Object>> dataList = new ArrayList<>();
		Map data = new HashMap();
		data.put("name",csm);
		data.put("id",mycity);
		dataList.add(data);
		new BaseBean().writeLog("所在城市(dataList)");
		new BaseBean().writeLog(JSON.toJSONString(dataList));
		BrowserBean bcp = citysci.getBrowserConditionParam();
		bcp.setReplaceDatas(dataList);
		citysci.setBrowserConditionParam(bcp);
		citysci.setValue(mycity);

/*
		itemlist.add(FieldUtil.getFormItemForBrowser(citysci, "mycity", SystemEnv.getHtmlLabelName(	528825, languageid), "58", mycity+"",viewAttr));
*/
		new BaseBean().writeLog("======GetRoomFieldsCmd(itemList)====");
		new BaseBean().writeLog(JSON.toJSONString(itemlist));
		itemlist.add(citysci);
		itemlist.add(FieldUtil.getFormItemForInput("mybuilding", SystemEnv.getHtmlLabelName(528827, Util.getIntValue(user.getLanguage())), mybuilding, viewAttr));
		itemlist.add(FieldUtil.getFormItemForInput("myfloor", SystemEnv.getHtmlLabelName(528826, Util.getIntValue(user.getLanguage())), myfloor, viewAttr));

		groupitem.put("title", SystemEnv.getHtmlLabelName(528828, user.getLanguage()));
		groupitem.put("defaultshow", true);
		groupitem.put("items", itemlist);
		grouplist.add(groupitem);

		resMap.put("hideField", hideFieldMap);
		resMap.put("hideColumn", hideColumnlist);
		resMap.put("fields", grouplist);

		resMap.put("allowMaxNum", allowMaxNum);
		return resMap;
	}

	public List getRoomAttributeOption(int languageid) {
		List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
		options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(528753, languageid),true));
		options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(528754, languageid)));
		options.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(528755, languageid)));
		return options;
	}

    public List getNumChkOption(int languageid) {
        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
        options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(528993, languageid), true));
        options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(528992, languageid)));
        return options;
    }

	public Map setLayout(Map item, int col) {
		item.put("fieldcol", col);
		return item;
	}



}
