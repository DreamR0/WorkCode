package com.engine.workflow.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.api.customization.zj0601.util.customBaseUtil;
import weaver.filter.WeaverRequest;
import weaver.general.BaseBean;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;

import com.alibaba.fastjson.JSONObject;
import com.engine.common.util.ServiceUtil;
import com.engine.workflow.service.RequestListService;
import com.engine.workflow.service.impl.RequestListServiceImpl;
import com.engine.workflow.util.CommonUtil;

import java.util.Map;

/**
 * 列表Action
 * @author liuzy 2018/5/9
 */
public class RequestListAction {

	private RequestListService getService(HttpServletRequest request, HttpServletResponse response) {
		User user = CommonUtil.getUserByRequest(request, response);
		return (RequestListService) ServiceUtil.getService(RequestListServiceImpl.class, user);
	}

	@POST
	@Path("/doingBaseInfo")
	@Produces(MediaType.TEXT_PLAIN)
	public String doingBaseInfo(@Context HttpServletRequest request, @Context HttpServletResponse response){
		return JSONObject.toJSONString(getService(request, response).doingBaseInfo(request));
	}

	@POST
	@Path("/doingCountInfo")
	@Produces(MediaType.TEXT_PLAIN)
	public String doingCountInfo(@Context HttpServletRequest request, @Context HttpServletResponse response){
		return JSONObject.toJSONString(getService(request, response).doingCountInfo(request));
	}

	@POST
	@Path("/doneBaseInfo")
	@Produces(MediaType.TEXT_PLAIN)
	public String doneBaseInfo(@Context HttpServletRequest request, @Context HttpServletResponse response){
		return JSONObject.toJSONString(getService(request, response).doneBaseInfo(request));
	}

	@POST
	@Path("/doneCountInfo")
	@Produces(MediaType.TEXT_PLAIN)
	public String doneCountInfo(@Context HttpServletRequest request, @Context HttpServletResponse response){
		return JSONObject.toJSONString(getService(request, response).doneCountInfo(request));
	}

	@POST
	@Path("/mineBaseInfo")
	@Produces(MediaType.TEXT_PLAIN)
	public String mineBaseInfo(@Context HttpServletRequest request, @Context HttpServletResponse response){
		return JSONObject.toJSONString(getService(request, response).mineBaseInfo(request));
	}

	@POST
	@Path("/mineCountInfo")
	@Produces(MediaType.TEXT_PLAIN)
	public String mineCountInfo(@Context HttpServletRequest request, @Context HttpServletResponse response){
		return JSONObject.toJSONString(getService(request, response).mineCountInfo(request));
	}

	@POST
	@Path("/splitPageKey")
	@Produces(MediaType.TEXT_PLAIN)
	public String splitPageKey(@Context HttpServletRequest request, @Context HttpServletResponse response){
		WeaverRequest weaverRequest = new WeaverRequest(request);
		return JSONObject.toJSONString(getService(request, response).splitPageKey(weaverRequest));
	}

	/** 获取全部待办未读数 **/
	@POST
	@Path("/getDoingCount")
	@Produces(MediaType.TEXT_PLAIN)
	public String getDoingCount(@Context HttpServletRequest request, @Context HttpServletResponse response){
		return JSONObject.toJSONString(getService(request, response).getDoingNewCount(request));
	}

	/** 批量置为主读 **/
	@POST
	@Path("/doReadIt")
	@Produces(MediaType.TEXT_PLAIN)
	public String doReadIt(@Context HttpServletRequest request, @Context HttpServletResponse response){
		return JSONObject.toJSONString(getService(request, response).doReadIt(request));
	}

	/**
	 * 保存默认排序列设置
	 * @param request
	 * @param response
	 * @return
	 */
	@POST
	@Path("/doSaveData")
	@Produces(MediaType.TEXT_PLAIN)
	public String doSaveData(@Context HttpServletRequest request, @Context HttpServletResponse response){
		return JSONObject.toJSONString(getService(request, response).doSaveData(request));
	}

	@POST
	@Path("/getDefaultList")
	@Produces(MediaType.TEXT_PLAIN)
	public String getDefaultList(@Context HttpServletRequest request, @Context HttpServletResponse response){
		return JSONObject.toJSONString(getService(request, response).getDefaultList(request));
	}

	@POST
	@Path("/getUnoperators")
	@Produces(MediaType.TEXT_PLAIN)
	public String getUnoperatorList(@Context HttpServletRequest request, @Context HttpServletResponse response){
		return JSONObject.toJSONString(getService(request, response).getUnoperators(request));
	}

	@POST
	@Path("/getWfListParams")
	@Produces(MediaType.TEXT_PLAIN)
	public String getWfListParams(@Context HttpServletRequest request, @Context HttpServletResponse response){
		//WeaverRequest weaverRequest = new WeaverRequest(request);
		return JSONObject.toJSONString(getService(request, response).getWfListParams(request));
	}

	/**
	 * 获取指定用户不在待办中的requestid
	 * @param request
	 * @param response
	 * @return
	 */
	@POST
	@Path("/getHendledReqIds")
	@Produces(MediaType.TEXT_PLAIN)
	public String getHendledReqIds(@Context HttpServletRequest request, @Context HttpServletResponse response){
		return JSONObject.toJSONString(getService(request, response).getHendledReqIds(request));
	}
	
	@POST
	@Path("/continnuationProcessInfo")
	@Produces(MediaType.TEXT_PLAIN)
	public String continnuationProcessInfo(@Context HttpServletRequest request, @Context HttpServletResponse response){
		//WeaverRequest weaverRequest = new WeaverRequest(request);
		return JSONObject.toJSONString(getService(request, response).continnuationProcessInfo(request));
	}

	@POST
	@Path("/judgeReloadList")
	@Produces(MediaType.TEXT_PLAIN)
	public String judgeReloadList(@Context HttpServletRequest request, @Context HttpServletResponse response){
		//WeaverRequest weaverRequest = new WeaverRequest(request);
		return JSONObject.toJSONString(getService(request, response).judgeReloadList(request));
	}


	/**
	 * 自定义页面（高级搜索条件）
	 * @param request
	 * @param response
	 * @return
	 */
	@POST
	@Path("/doShowBaseInfo")
	@Produces(MediaType.TEXT_PLAIN)
	public String doShowBaseInfo(@Context HttpServletRequest request, @Context HttpServletResponse response){
		customBaseUtil customBaseUtil = new customBaseUtil();
		Map<String,Object> result = null;
		try{
			User user = HrmUserVarify.getUser(request,response);
			result = customBaseUtil.getBaseInfo(request,user);

		}catch (Exception e){
			new BaseBean().writeLog("customBaseInfo error:"+e);
		}
		return JSONObject.toJSONString(result);
	}

	/**
	 * 自定义页面(tab标签)
	 * @param request
	 * @param response
	 * @return
	 */
	@POST
	@Path("/doShowCountInfo")
	@Produces(MediaType.APPLICATION_JSON)
	public String doShowCountInfo(@Context HttpServletRequest request, @Context HttpServletResponse response){
		customBaseUtil customBaseUtil = new customBaseUtil();
		Map<String,Object> result = null;
		try{
			User user = HrmUserVarify.getUser(request,response);
			result = customBaseUtil.getCountInfo(request,user);

		}catch (Exception e){
			new BaseBean().writeLog("customCountInfo error:"+e);
			result.put("code",-1);
			result.put("msg",e);
		}
		return JSONObject.toJSONString(result);
	}
}
