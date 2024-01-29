package weaver.hrm;

/**
 * Title:        Cobusiness
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:      weaver
 * @author liuyu
 * @version 1.0
 */

import java.util.*;

import javax.servlet.http.*;

import com.alibaba.fastjson.JSON;
import com.customization.qc2269712.CheckPermissionUtil;
import weaver.systeminfo.setting.HrmUserSettingComInfo;
import weaver.systeminfo.systemright.CheckUserRight;
import weaver.general.*;
import weaver.hrm.online.HrmUserOnlineMap;
import weaver.conn.*;
import weaver.login.LicenseCheckLogin;
/**
 * 用户验正类
 */
public class HrmUserVarify{

	/**  判断user对象是否存在于session中。
	 *  @param  HttpServletRequest request
	 *  @param  HttpServletResponse response
	 *  @return  User
	 */

	public static User getUserOld(HttpServletRequest request, HttpServletResponse response) {
		User user = (User) request.getSession(true).getAttribute("weaver_user@bean");
		//E9 User对象为空拦截已在sessionfilter处理
//		if (user == null) {
//			//String loginid = Util.getCookie(request , "loginidweaver") ;
//			String loginfile = Util.getCookie(request, "loginfileweaver");
//			//user = getUserfromDB(loginid) ;
//			//request.getSession(true).setMaxInactiveInterval(1800);
//			//request.getSession(true).setAttribute("weaver_user@bean",user) ;
//			//request.getSession(true).setAttribute("moniter",new OnLineMonitor()) ;
//			try {
//				if (Util.null2String(loginfile).equals("") || Util.null2String(loginfile).toLowerCase().equals("null"))
//					//response.sendRedirect("/login/Login.jsp");
//					response.sendRedirect("/wui/index.html");
//				else
//					//response.sendRedirect("/Refresh.jsp?loginfile=" + loginfile + "&message=19");
//					response.sendRedirect("/wui/index.html");
//			} catch (Exception er) {
//			}
//
//			//添加超时标识 (TD 2227)
//			//hubo,050707
//			//request.getSession(true).setAttribute("istimeout","yes");
//		}

		return user;
	}

	/***
	 * 获取user
	 * @param request
	 * @return
	 */
	public static User getUser4WF(HttpServletRequest request, HashMap<String,Object> params) {
		User user = (User) request.getSession(true).getAttribute("weaver_user@bean");
		int f_weaver_belongto_userid = Util.getIntValue(Util.null2String(params.get("f_weaver_belongto_userid")),0);
		int f_weaver_belongto_usertype = Util.getIntValue(Util.null2String(params.get("f_weaver_belongto_usertype")),0);
		int languageId = 7;
		if(user!=null){
			languageId = user.getLanguage();
		}
		if(languageId<7) languageId=7;
		RecordSet rs = new RecordSet();
		if(user!=null&&f_weaver_belongto_userid!=user.getUID()&&f_weaver_belongto_userid>0){
			//判断是否为主次账号关系
			boolean isBelongto = false;
			String sql = " SELECT belongto FROM HrmResource WHERE id= "+user.getUID();
			rs.executeSql(sql);
			while(rs.next()){
				int belongto = rs.getInt("belongto");
				if(belongto == f_weaver_belongto_userid){
					isBelongto=true;
					break;
				}
			}

			sql = " SELECT id FROM HrmResource WHERE belongto= "+user.getUID();
			rs.executeSql(sql);
			while(rs.next()){
				int id = rs.getInt("id");
				if(id == f_weaver_belongto_userid){
					isBelongto=true;
					break;
				}
			}
			if(isBelongto){
				user= User.getUser(f_weaver_belongto_userid, f_weaver_belongto_usertype);
				if(user.getLanguage()!=languageId){
					user.setUserLang(f_weaver_belongto_userid, languageId);
				}
			}
		}

		return user;
	}

	/**  判断user对象是否存在于session中。
	 *  @param  HttpServletRequest request
	 *  @param  HttpServletResponse response
	 *  @return  User
	 */
	public static User getUser(HttpServletRequest request, HttpServletResponse response) {
		User user = (User) request.getSession(true).getAttribute("weaver_user@bean");
//		if (user == null) {
//			String loginfile = Util.getCookie(request, "loginfileweaver");
//			try {
//				if (Util.null2String(loginfile).equals("") || Util.null2String(loginfile).toLowerCase().equals("null")){
//					//response.sendRedirect("/login/Login.jsp");
//					response.sendRedirect("/wui/index.html");
//					return null;
//				}else{
//					response.sendRedirect("/wui/index.html");
//					//response.sendRedirect("/Refresh.jsp?loginfile=" + loginfile + "&message=19");
//					return null;
//				}
//			} catch (Exception er) {
//			}
//		}
		int languageId = 7;
		if(user!=null){
			languageId = user.getLanguage();
		}
		if(languageId<7) languageId=7;
		int f_weaver_belongto_userid = Util.getIntValue(request.getParameter("f_weaver_belongto_userid"),0);
		int f_weaver_belongto_usertype = Util.getIntValue(request.getParameter("f_weaver_belongto_usertype"),0);
		RecordSet rs = new RecordSet();
		if(user!=null&&f_weaver_belongto_userid!=user.getUID()&&f_weaver_belongto_userid>0){
			//判断是否为主次账号关系
			boolean isBelongto = false;
			String sql = " SELECT belongto FROM HrmResource WHERE id= "+user.getUID();
			rs.executeSql(sql);
			while(rs.next()){
				int belongto = rs.getInt("belongto");
				if(belongto == f_weaver_belongto_userid){
					isBelongto=true;
					break;
				}
			}

			sql = " SELECT id FROM HrmResource WHERE belongto= "+user.getUID();
			rs.executeSql(sql);
			while(rs.next()){
				int id = rs.getInt("id");
				if(id == f_weaver_belongto_userid){
					isBelongto=true;
					break;
				}
			}

			if(isBelongto){
				user= User.getUser(f_weaver_belongto_userid, f_weaver_belongto_usertype);
				if(user.getLanguage()!=languageId){
					user.setUserLang(f_weaver_belongto_userid, languageId);
				}
			}
		}

		return user;
	}

	/***
	 *
	 * @param request
	 * @param response
	 * @param f_weaver_belongto_userid 次账号id
	 * @param f_weaver_belongto_usertype
	 * @return
	 */
	public static User getUser(HttpServletRequest request, HttpServletResponse response, String f_weaver_belongto_userid, String f_weaver_belongto_usertype) {
		return getUser(request, response, Util.getIntValue(f_weaver_belongto_userid,0), Util.getIntValue(f_weaver_belongto_usertype,0));
	}
	/***
	 *
	 * @param request
	 * @param response
	 * @param f_weaver_belongto_userid 次账号id
	 * @param f_weaver_belongto_usertype
	 * @return
	 */
	public static User getUser(HttpServletRequest request, HttpServletResponse response, int f_weaver_belongto_userid, int f_weaver_belongto_usertype) {
		User user = (User) request.getSession(true).getAttribute("weaver_user@bean");
//		if (user == null) {
//			String loginfile = Util.getCookie(request, "loginfileweaver");
//			try {
//				if (Util.null2String(loginfile).equals("") || Util.null2String(loginfile).toLowerCase().equals("null")){
//					response.sendRedirect("/wui/index.html");
//					//response.sendRedirect("/login/Login.jsp");
//					return null;
//				}else{
//					response.sendRedirect("/wui/index.html");
//					//response.sendRedirect("/Refresh.jsp?loginfile=" + loginfile + "&message=19");
//					return null;
//				}
//			} catch (Exception er) {
//			}
//		}
		int languageId = 7;
		if(user!=null){
			languageId = user.getLanguage();
		}
		if(languageId<7) languageId=7;
		RecordSet rs = new RecordSet();
		if(user!=null&&f_weaver_belongto_userid!=user.getUID()&&f_weaver_belongto_userid>0){
			//判断是否为主次账号关系
			boolean isBelongto = false;
			String sql = " SELECT belongto FROM HrmResource WHERE id= "+user.getUID();
			rs.executeSql(sql);
			while(rs.next()){
				int belongto = rs.getInt("belongto");
				if(belongto == f_weaver_belongto_userid){
					isBelongto=true;
					break;
				}
			}

			sql = " SELECT id FROM HrmResource WHERE belongto= "+user.getUID();
			rs.executeSql(sql);
			while(rs.next()){
				int id = rs.getInt("id");
				if(id == f_weaver_belongto_userid){
					isBelongto=true;
					break;
				}
			}

			if(isBelongto){
				user= User.getUser(f_weaver_belongto_userid, f_weaver_belongto_usertype);
				if(user.getLanguage()!=languageId){
					user.setUserLang(f_weaver_belongto_userid, languageId);
				}
			}
		}

		return user;
	}
    /**
     * 从sesson中获取user对象
     * @param request
     * @param response
     * @return
     */
    public static User checkUser(HttpServletRequest request, HttpServletResponse response) {
        User user = (User)request.getSession(true).getAttribute("weaver_user@bean") ;
        return user;
    }

    /**  判断用户是否具有操作权限(如有此账号，增加次账号判断)
     *  @param  String processname   操作
     *  @param  User user  用户
     *  @return  boolean
     */

    public static boolean checkUserRight(String processname, User user) {
        if(user == null)return false;
      if(user.getLoginid().equalsIgnoreCase("sysadmin")) return true ;
      //==zj 这里多判断一层，如果角色HQ(总部角色)，就拥有所有组织操作权限
		CheckPermissionUtil checkPermissionUtil = new CheckPermissionUtil();
		new BaseBean().writeLog("==zj==(该角色是否有操作权限)" + checkPermissionUtil.checkHead(user));
		if (checkPermissionUtil.checkHead(user)){
			return true;
		}

		CheckUserRight ck = new CheckUserRight() ;
  		boolean hasRight= ck.checkUserRight(processname, user);

  		/*************判断次账号 begin***************/
  		if (!hasRight) {
  			String belongtoshow = "";// 是否启用多账号数据显示
				try {
					HrmUserSettingComInfo HrmUserSettingComInfo = new HrmUserSettingComInfo();
					belongtoshow = HrmUserSettingComInfo.getBelongtoshowByUserId("" + user.getUID());
				} catch (Exception e) {
				}

				if (belongtoshow.equals("1")) {
					List lsUser = User.getBelongtoUsersByUserId(user.getUID());// 所有此账号
					if(lsUser!=null){
						for (Object tmpUser : lsUser) {
							hasRight = ck.checkUserRight(processname, (User) tmpUser);
							if (hasRight) {
								break;
							}
						}
					}
				}
  		}
  		/** ************判断次账号 end************** */

      return hasRight ;
    }

    /**  获得具有操作权限用户ID
     *  @param  String processname   操作
     *  @param  User user  用户
     *  @return  userid
     */
    public static String getcheckUserRightUserId(String processname, User user) {
    	return getcheckUserRightUserId(processname, user, true);
    }

    /**  获得具有操作权限用户ID
     *  @param  String processname   操作
     *  @param  User user  用户
     *  @return  userid
     */
    private static String getcheckUserRightUserId(String processname, User user, boolean checkAdmin) {

    	if(checkAdmin && user.getLoginid().equalsIgnoreCase("sysadmin")) return "1" ;
      String hasRightUserid = "";
    	CheckUserRight ck = new CheckUserRight() ;
    	boolean hasRight= ck.checkUserRight(processname, user);
    	if(hasRight)return ""+user.getUID();

  		String belongtoshow = "";//是否启用多账号数据显示
  		try{
  			HrmUserSettingComInfo HrmUserSettingComInfo = new HrmUserSettingComInfo();
  			belongtoshow = HrmUserSettingComInfo.getBelongtoshowByUserId(""+user.getUID());
  		}catch (Exception e) {}

  		if(belongtoshow.equals("1")){
        List lsUser = User.getBelongtoUsersByUserId(user.getUID());//所有此账号
        if(lsUser!=null){
		      for(Object tmpUser :lsUser){
		      	hasRight = ck.checkUserRight(processname, (User)tmpUser);
		      	if(hasRight){
		      		hasRightUserid = ""+((User)tmpUser).getUID();
		      		break;
		      	}
		  		}
        }
  		}

      return hasRightUserid ;
    }

		  /**  判断用户是否具有操作权限包括系统管理员
     *  @param  String processname   操作
     *  @param  User user  用户
     *  @return  boolean
     */

    public static boolean checkUserRightSystemadmin(String processname, User user) {

      CheckUserRight ck = new CheckUserRight() ;
      //return ck.checkUserRight(processname, user) ;

      boolean hasRight= ck.checkUserRight(processname, user);

  		/*************判断次账号 begin***************/
  		if (!hasRight) {
  			String belongtoshow = "";// 是否启用多账号数据显示
				try {
					HrmUserSettingComInfo HrmUserSettingComInfo = new HrmUserSettingComInfo();
					belongtoshow = HrmUserSettingComInfo.getBelongtoshowByUserId("" + user.getUID());
				} catch (Exception e) {
				}
				if (belongtoshow.equals("1")) {
					List lsUser = User.getBelongtoUsersByUserId(user.getUID());// 所有此账号
					if(lsUser!=null){
						for (Object tmpUser : lsUser) {
							hasRight = ck.checkUserRight(processname, (User) tmpUser);
							if (hasRight) {
								break;
							}
						}
					}
				}
  		}
  		/** ************判断次账号 end************** */

      return hasRight ;
    }

    /**  获得具有操作权限用户ID 包括系统管理员
     *  @param  String processname   操作
     *  @param  User user  用户
     *  @return  userid
     */
    public static String getcheckUserRightSystemadminUserId(String processname, User user) {
    	return getcheckUserRightUserId(processname, user, false);
    }

    /**
     *   判断用户在指定部门下是否具有操作权限
     * @param processname 操作
     * @param user   用户
     * @param departmentid   部门
     * @return
     */
    public static boolean checkUserRight(String processname, User user, String departmentid) {
      if(user.getLoginid().equalsIgnoreCase("sysadmin")) return true ;
      CheckUserRight ck = new CheckUserRight() ;
      //return ck.checkUserRight(processname, user,departmentid) ;

      boolean hasRight= ck.checkUserRight(processname, user,departmentid) ;

  		/*************判断次账号 begin***************/
  		if (!hasRight) {
  			String belongtoshow = "";// 是否启用多账号数据显示
				try {
					HrmUserSettingComInfo HrmUserSettingComInfo = new HrmUserSettingComInfo();
					belongtoshow = HrmUserSettingComInfo.getBelongtoshowByUserId("" + user.getUID());
				} catch (Exception e) {
				}
				if (belongtoshow.equals("1")) {
					List lsUser = User.getBelongtoUsersByUserId(user.getUID());// 所有此账号
					if(lsUser!=null){
						for (Object tmpUser : lsUser) {
							hasRight = ck.checkUserRight(processname, (User) tmpUser,departmentid);
							if (hasRight) {
								break;
							}
						}
					}
				}
  		}
  		/** ************判断次账号 end************** */

      return hasRight ;
    }

    /**  获得具有操作权限用户ID
     *  @param  String processname   操作
     *  @param  User user  用户
     *  @return  userid
     */
    public static String getcheckUserRightUserId(String processname, User user, String departmentid) {
      String hasRightUserid = "";
    	if(user.getLoginid().equalsIgnoreCase("sysadmin")) return "1" ;
    	CheckUserRight ck = new CheckUserRight() ;
    	boolean hasRight= ck.checkUserRight(processname, user, departmentid);
    	if(hasRight)return ""+user.getUID();


  		String belongtoshow = "";//是否启用多账号数据显示
  		try{
  			HrmUserSettingComInfo HrmUserSettingComInfo = new HrmUserSettingComInfo();
  			belongtoshow = HrmUserSettingComInfo.getBelongtoshowByUserId(""+user.getUID());
  		}catch (Exception e) {}

  		if(belongtoshow.equals("1")){
        List lsUser = User.getBelongtoUsersByUserId(user.getUID());//所有此账号
        if(lsUser!=null){
	        for(Object tmpUser :lsUser){
		      	hasRight = ck.checkUserRight(processname, (User)tmpUser, departmentid);
		      	if(hasRight){
		      		hasRightUserid = ""+((User)tmpUser).getUID();
		      		break;
		      	}
		  		}
        }
  		}

      return hasRightUserid ;
    }

    /**
     *  判断用户在指定部门下是否具有操作权限
     * @param processname   操作
     * @param user  用户
     * @param departmentid   部门
     * @return
     */
    public static boolean checkUserRight(String processname, User user, int departmentid) {
      return checkUserRight(processname, user, ""+departmentid) ;
    }

    public static String getcheckUserRightUserId(String processname, User user, int departmentid) {
    	return getcheckUserRightUserId(processname, user, ""+departmentid) ;
    }


    /**
     * 判断权限级别 增加此账号判断 返回最大级别
     * @param processname   操作
     * @param user   用户
     * @return
     */
    public static String getRightLevel(String processname, User user) {
    	if(user.getLoginid().equalsIgnoreCase("sysadmin")) return "2" ;
      CheckUserRight ck = new CheckUserRight() ;
      //return ck.getRightLevel(processname,user) ;

      String rightLevel= ck.getRightLevel(processname, user);
      int maxRightLevel = Util.getIntValue(rightLevel,-1);
  		/*************判断次账号 begin***************/
			String belongtoshow = "";// 是否启用多账号数据显示
			try {
				HrmUserSettingComInfo HrmUserSettingComInfo = new HrmUserSettingComInfo();
				belongtoshow = HrmUserSettingComInfo.getBelongtoshowByUserId("" + user.getUID());
			} catch (Exception e) {
			}
			if (belongtoshow.equals("1")) {
				List lsUser = User.getBelongtoUsersByUserId(user.getUID());// 所有此账号
				if(lsUser!=null){
					for (Object tmpUser : lsUser) {
						rightLevel = ck.getRightLevel(processname, (User) tmpUser);
						if (!rightLevel.equals("-1")) {
							int iRightLevel = Util.getIntValue(rightLevel,-1);
							if(iRightLevel>maxRightLevel)maxRightLevel = iRightLevel;
						}
					}
				}
			}
  		/** ************判断次账号 end************** */

      return ""+maxRightLevel ;
    }

    /**
     *    判断权限级别
     * @param resourceid    人员
     * @param roleid   角色
     * @return
     */
    public String getRightLevel(String resourceid , String roleid) {
      CheckUserRight ck = new CheckUserRight() ;
      //return ck.getRightLevel(resourceid , roleid) ;

      String rightLevel= ck.getRightLevel(resourceid, roleid);
      int maxRightLevel = Util.getIntValue(rightLevel,-1);

  		/*************判断次账号 begin***************/
			String belongtoshow = "";// 是否启用多账号数据显示
			try {
				HrmUserSettingComInfo HrmUserSettingComInfo = new HrmUserSettingComInfo();
				belongtoshow = HrmUserSettingComInfo.getBelongtoshowByUserId(resourceid);
			} catch (Exception e) {
			}
			if (belongtoshow.equals("1")) {
				List lsUser = User.getBelongtoUsersByUserId(resourceid);// 所有此账号
				if(lsUser!=null){
					for (Object tmpUser : lsUser) {
						rightLevel = ck.getRightLevel(resourceid, (User) tmpUser);
						if (!rightLevel.equals("-1")) {
							int iRightLevel = Util.getIntValue(rightLevel,-1);
							if(iRightLevel>maxRightLevel)maxRightLevel = iRightLevel;
						}
					}
				}
			}

  		/** ************判断次账号 end************** */

      return ""+maxRightLevel ;
    }

    /**
     *   判断用户是否具有操作权限
     * @param resourceid     人员
     * @param roleid    角色
     * @param rolelevel    角色级别
     * @return
     */
    public boolean checkUserRight(String resourceid , String roleid , String rolelevel) {
      CheckUserRight ck = new CheckUserRight() ;
      //return ck.checkUserRight(resourceid , roleid, rolelevel) ;
      boolean hasRight= ck.checkUserRight(resourceid , roleid, rolelevel) ;

  		/*************判断次账号 begin***************/
  		if (!hasRight) {
  			String belongtoshow = "";// 是否启用多账号数据显示
				try {
					HrmUserSettingComInfo HrmUserSettingComInfo = new HrmUserSettingComInfo();
					belongtoshow = HrmUserSettingComInfo.getBelongtoshowByUserId(resourceid);
				} catch (Exception e) {
				}
				if (belongtoshow.equals("1")) {
					List lsUser = User.getBelongtoUsersByUserId(resourceid);// 所有此账号
					if(lsUser!=null){
						for (Object tmpUser : lsUser) {
							hasRight = ck.checkUserRight(""+((User)tmpUser).getUID() , roleid, rolelevel);
							if (hasRight) {
								break;
							}
						}
					}
				}
  		}
  		/** ************判断次账号 end************** */
  		return hasRight;
    }

    /**  获得具有操作权限用户ID
     *  @param  String processname   操作
     *  @param  User user  用户
     *  @return  userid
     */
    public static String getcheckUserRightUserId(String resourceid , String roleid , String rolelevel) {
      String hasRightUserid = "";
      CheckUserRight ck = new CheckUserRight() ;
    	boolean hasRight= ck.checkUserRight(resourceid , roleid , rolelevel);
    	if(hasRight)return ""+resourceid;


  		String belongtoshow = "";//是否启用多账号数据显示
  		try{
  			HrmUserSettingComInfo HrmUserSettingComInfo = new HrmUserSettingComInfo();
  			belongtoshow = HrmUserSettingComInfo.getBelongtoshowByUserId(resourceid);
  		}catch (Exception e) {}
  		if(belongtoshow.equals("1")){
        List lsUser = User.getBelongtoUsersByUserId(resourceid);//所有此账号
        if(lsUser!=null){
		      for(Object tmpUser :lsUser){
		      	hasRight = new CheckUserRight().checkUserRight(""+((User)tmpUser).getUID() , roleid , rolelevel);
		      	if(hasRight){
		      		hasRightUserid = ""+((User)tmpUser).getUID();
		      		break;
		      	}
		  		}
        }
  		}

      return hasRightUserid ;
    }

    /**
     *    判断用户是否具有操作权限
     * @param roleid  角色
     * @param user 用户
     * @param departmentid   部门
     * @return
     */
    public boolean checkUserRole(String roleid , User user, String departmentid) {
      CheckUserRight ck = new CheckUserRight() ;
      //return ck.checkUserRole(roleid , user, departmentid) ;

      boolean hasRight= ck.checkUserRole(roleid , user, departmentid) ;

  		/*************判断次账号 begin***************/
  		if (!hasRight) {
  			String belongtoshow = "";// 是否启用多账号数据显示
				try {
					HrmUserSettingComInfo HrmUserSettingComInfo = new HrmUserSettingComInfo();
					belongtoshow = HrmUserSettingComInfo.getBelongtoshowByUserId(""+user.getUID());
				} catch (Exception e) {
				}
				if (belongtoshow.equals("1")) {
					List lsUser = User.getBelongtoUsersByUserId(user.getUID());// 所有此账号
					if(lsUser!=null){
						for (Object tmpUser : lsUser) {
							hasRight = ck.checkUserRole(roleid , (User)tmpUser, departmentid);
							if (hasRight) {
								break;
							}
						}
					}
				}
  		}
  		/** ************判断次账号 end************** */
  		return hasRight;
    }

    /**  获得具有操作权限用户ID
     *  @param  String processname   操作
     *  @param  User user  用户
     *  @return  userid
     */
    public static String getcheckUserRoleUserId(String roleid , User user, String departmentid) {
      String hasRightUserid = "";
      CheckUserRight ck = new CheckUserRight() ;
    	boolean hasRight= ck.checkUserRight(roleid , user , departmentid);
    	if(hasRight)return ""+user.getUID();


  		String belongtoshow = "";//是否启用多账号数据显示
  		try{
  			HrmUserSettingComInfo HrmUserSettingComInfo = new HrmUserSettingComInfo();
  			belongtoshow = HrmUserSettingComInfo.getBelongtoshowByUserId(""+user.getUID());
  		}catch (Exception e) {}

  		if(belongtoshow.equals("1")){
        List lsUser = User.getBelongtoUsersByUserId(user.getUID());//所有此账号
        if(lsUser!=null){
		      for(Object tmpUser :lsUser){
		      	hasRight = ck.checkUserRight(roleid , (User)tmpUser , departmentid);
		      	if(hasRight){
		      		hasRightUserid = ""+((User)tmpUser).getUID();
		      		break;
		      	}
		  		}
        }
  		}

      return hasRightUserid ;
    }

    /**
     * 判断用户是否在线
     * @param resourceid 人员id
     * @return
     */
    public static boolean isUserOnline(String resourceid)  {

      //移除不在线人员
//      LicenseCheckLogin lck = new LicenseCheckLogin();
//      lck.checkOnlineUser();
//
//      StaticObj staticobj = StaticObj.getInstance();
//      ArrayList onlineuserids = (ArrayList)staticobj.getObject("onlineuserids") ;
//      if(onlineuserids == null ) return false ;
//      int index = onlineuserids.indexOf(resourceid) ;
//      if(index == -1) return false ;
//      return true ;
			HrmUserOnlineMap hrmUserOnlineMap = HrmUserOnlineMap.getInstance();
			return hrmUserOnlineMap.isOnline(Util.getIntValue(resourceid)) ;
    }
    /**
     * 获取在线用户的IP地址
     * @param resourceid
     * @return
     */
	public static String getOnlineUserIp(String resourceid)
	{
		return HrmUserOnlineMap.getInstance().getClientIpByUidFromClusterMap(resourceid);
	}
    private static User getUserfromDB(String loginid) {
      RecordSet rs = new RecordSet() ;
      User user = new User() ;
		rs.execute("SELECT id,firstname,lastname,systemlanguage,seclevel FROM HrmResourceManager WHERE loginid='"+loginid+"'");
		if(rs.next()){
			user.setUid(rs.getInt("id"));
			user.setLoginid(loginid);
			user.setFirstname(rs.getString("firstname"));
			user.setLastname(rs.getString("lastname"));
			user.setLanguage(Util.getIntValue(rs.getString("systemlanguage"),7));
			user.setSeclevel(rs.getString("seclevel"));
			user.setLogintype("1");
		}
      rs.execute("HrmResource_SelectByLoginID",loginid);
      if(rs.next()) {
		user.setUid(rs.getInt("id"));
        user.setLoginid(loginid);
        user.setFirstname(rs.getString("firstname"));
        user.setLastname(rs.getString("lastname"));
        user.setAliasname(rs.getString("aliasname"));
        user.setTitle(rs.getString("title"));
        user.setTitlelocation(rs.getString("titlelocation"));
        user.setSex(rs.getString("sex"));
        user.setLanguage(Util.getIntValue(rs.getString("systemlanguage"),7));
        user.setTelephone(rs.getString("telephone"));
        user.setMobile(rs.getString("mobile"));
        user.setMobilecall(rs.getString("mobilecall"));
        user.setEmail(rs.getString("email"));
        user.setCountryid(rs.getString("countryid"));
        user.setLocationid(rs.getString("locationid"));
        user.setResourcetype(rs.getString("resourcetype"));
        user.setStartdate(rs.getString("startdate"));
        user.setEnddate(rs.getString("enddate"));
        user.setContractdate(rs.getString("contractdate"));
        user.setJobtitle(rs.getString("jobtitle"));
        user.setJobgroup(rs.getString("jobgroup"));
        user.setJobactivity(rs.getString("jobactivity"));
        user.setJoblevel(rs.getString("joblevel"));
        user.setSeclevel(rs.getString("seclevel"));
        user.setUserDepartment(Util.getIntValue(rs.getString("departmentid"),0));
        user.setUserSubCompany1(Util.getIntValue(rs.getString("subcompanyid1"),0));
        user.setUserSubCompany2(Util.getIntValue(rs.getString("subcompanyid2"),0));
        user.setUserSubCompany3(Util.getIntValue(rs.getString("subcompanyid3"),0));
        user.setUserSubCompany4(Util.getIntValue(rs.getString("subcompanyid4"),0));
        user.setManagerid(rs.getString("managerid"));
        user.setAssistantid(rs.getString("assistantid"));
        user.setPurchaselimit(rs.getString("purchaselimit"));
        user.setCurrencyid(rs.getString("currencyid"));
        user.setLastlogindate(rs.getString("lastlogindate"));
		user.setLogintype("1");
	  }
	  return user ;
	}
}
