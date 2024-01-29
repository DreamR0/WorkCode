package com.engine.kq.biz;

import weaver.cache.CacheBase;
import weaver.cache.CacheColumn;
import weaver.cache.CacheColumnType;
import weaver.cache.PKColumn;
import weaver.general.Util;

public class KQGroupComInfo extends CacheBase{
	protected static String TABLE_NAME = "kq_group";
	/** sql中的where信息，不要以where开始 */
	protected static String TABLE_WHERE = null;
	/** sql中的order by信息，不要以order by开始 */
	protected static String TABLE_ORDER = "id";

	@PKColumn(type = CacheColumnType.NUMBER)
	protected static String PK_NAME = "id";

	@CacheColumn(name = "groupname")
	protected static int groupname;

	@CacheColumn(name = "excludeid")
	protected static int excludeid;

	@CacheColumn(name = "excludecount")
	protected static int excludecount;

	@CacheColumn(name = "subcompanyid")
	protected static int subcompanyid;

	@CacheColumn(name = "kqtype")
	protected static int kqtype;

	@CacheColumn(name = "serialids")
	protected static int serialids;

	@CacheColumn(name = "weekday")
	protected static int weekday;

	@CacheColumn(name = "signstart")
	protected static int signstart;

	@CacheColumn(name = "workhour")
	protected static int workhour;

	@CacheColumn(name = "isdelete")
	protected static int isdelete;

	@CacheColumn(name = "signintype")
	protected static int signintype;

	@CacheColumn(name = "ipscope")
	protected static int ipscope;

	@CacheColumn(name = "locationcheck")
	protected static int locationcheck;

	@CacheColumn(name = "locationcheckscope")
	protected static int locationcheckscope;

	@CacheColumn(name = "wificheck")
	protected static int wificheck;

	@CacheColumn(name = "outsidesign")
	protected static int outsidesign;

	@CacheColumn(name = "validity")
	protected static int validity;

	@CacheColumn(name = "validityfromdate")
	protected static int validityfromdate;

	@CacheColumn(name = "validityenddate")
	protected static int validityenddate;

	@CacheColumn(name = "locationfacecheck")
	protected static int locationfacecheck;

	@CacheColumn(name = "locationfacechecktype")
	protected static int locationfacechecktype;

	@CacheColumn(name = "locationshowaddress")
	protected static int locationshowaddress;

	@CacheColumn(name = "wififacecheck")
	protected static int wififacecheck;

	@CacheColumn(name = "wififacechecktype")
	protected static int wififacechecktype;

	@CacheColumn(name = "ipscope_v4_pc")
	protected static int ipscope_v4_pc;

	@CacheColumn(name = "ipscope_v4_em")
	protected static int ipscope_v4_em;

	@CacheColumn(name = "ipscope_v6_pc")
	protected static int ipscope_v6_pc;

	@CacheColumn(name = "ipscope_v6_em")
	protected static int ipscope_v6_em;

	@CacheColumn(name = "auto_checkin")
	protected static int auto_checkin;

	@CacheColumn(name = "auto_checkin_before")
	protected static int auto_checkin_before;

	@CacheColumn(name = "auto_checkin_after")
	protected static int auto_checkin_after;

	@CacheColumn(name = "auto_checkout")
	protected static int auto_checkout;

	@CacheColumn(name = "auto_checkout_before")
	protected static int auto_checkout_before;

	@CacheColumn(name = "auto_checkout_after")
	protected static int auto_checkout_after;

  @CacheColumn(name = "calmethod")
  protected static int calmethod;

	public String getId(){
		return (String)getRowValue(PK_INDEX);
	}

	public String getGroupname() { return (String)getRowValue(groupname); }

	public String getGroupname(String key)
	{
		return (String)getValue(groupname,key);
	}

	public String getExcludeid() { return (String)getRowValue(excludeid); }
	public String getExcludeid(String key)
	{
		return (String)getValue(excludeid,key);
	}

	public String getExcludecount() { return (String)getRowValue(excludecount); }
	public String getExcludecount(String key)
	{
		return (String)getValue(excludecount,key);
	}

	public String getSubcompanyid() { return (String)getRowValue(subcompanyid); }
	public String getSubcompanyid(String key)
	{
		return (String)getValue(subcompanyid,key);
	}

	public String getKqtype() { return (String)getRowValue(kqtype); }
	public String getKqtype(String key)
	{
		return (String)getValue(kqtype,key);
	}

	public String getSerialids() { return (String)getRowValue(serialids); }
	public String getSerialids(String key)
	{
		return (String)getValue(serialids,key);
	}

	public String getWeekday() { return (String)getRowValue(weekday); }
	public String getWeekday(String key)
	{
		return (String)getValue(weekday,key);
	}

	public String getSignstart() { return (String)getRowValue(signstart); }
	public String getSignstart(String key)
	{
		return (String)getValue(signstart,key);
	}

	public String getWorkhour() { return (String)getRowValue(workhour); }
	public String getWorkhour(String key)
	{
		return (String)getValue(workhour,key);
	}

	public String getIsdelete() { return (String)getRowValue(isdelete); }
	public String getIsdelete(String key)
	{
		return (String)getValue(isdelete,key);
	}

	public String getSignintype() { return (String)getRowValue(signintype); }
	public String getSignintype(String key)
	{
		return (String)getValue(signintype,key);
	}

	public String getIpscope() { return (String)getRowValue(ipscope); }
	public String getIpscope(String key)
	{
		return (String)getValue(ipscope,key);
	}

	public String getLocationcheck() { return (String)getRowValue(locationcheck); }
	public String getLocationcheck(String key)
	{
		return (String)getValue(locationcheck,key);
	}

	public String getLocationcheckscope() { return (String)getRowValue(locationcheckscope); }
	public String getLocationcheckscope(String key)
	{
		return (String)getValue(locationcheckscope,key);
	}

	public String getWificheck() { return (String)getRowValue(wificheck); }
	public String getWificheck(String key)
	{
		return (String)getValue(wificheck,key);
	}

	public String getOutsidesign() { return (String)getRowValue(outsidesign); }
	public String getOutsidesign(String key)
	{
		return (String)getValue(outsidesign,key);
	}

	public String getValidity() { return (String)getRowValue(validity); }
	public String getValidity(String key)
	{
		return (String)getValue(validity,key);
	}

	public String getValidityfromdate() { return (String)getRowValue(validityfromdate); }
	public String getValidityfromdate(String key)
	{
		return (String)getValue(validityfromdate,key);
	}

	public String getValidityenddate() { return (String)getRowValue(validityenddate); }
	public String getValidityenddate(String key)
	{
		return (String)getValue(validityenddate,key);
	}

	public String getLocationfacecheck() { return (String)getRowValue(locationfacecheck); }
	public String getLocationfacecheck(String key)
	{
		return (String)getValue(locationfacecheck,key);
	}

	public String getLocationfacechecktype() { return (String)getRowValue(locationfacechecktype); }
	public String getLocationfacechecktype(String key)
	{
		return (String)getValue(locationfacechecktype,key);
	}


	public String getLocationshowaddress() { return (String)getRowValue(locationshowaddress); }
	public String getLocationshowaddress(String key)
	{
		return (String)getValue(locationshowaddress,key);
	}

	public String getWififacecheck() { return (String)getRowValue(wififacecheck); }
	public String getWififacecheck(String key)
	{
		return (String)getValue(wififacecheck,key);
	}

	public String getWififacechecktype() { return (String)getRowValue(wififacechecktype); }
	public String getWififacechecktype(String key)
	{
		return (String)getValue(wififacechecktype,key);
	}

  public String getIpscope_v4_pc() { return (String)getRowValue(ipscope_v4_pc); }
  public String getIpscope_v4_pc(String key)
  {
    return (String)getValue(ipscope_v4_pc,key);
  }

  public String getIpscope_v4_em() { return (String)getRowValue(ipscope_v4_em); }
  public String getIpscope_v4_em(String key)
  {
    return (String)getValue(ipscope_v4_em,key);
  }
  public String getIpscope_v6_pc() { return (String)getRowValue(ipscope_v6_pc); }
  public String getIpscope_v6_pc(String key)
  {
    return (String)getValue(ipscope_v6_pc,key);
  }
  public String getIpscope_v6_em() { return (String)getRowValue(ipscope_v6_em); }
  public String getIpscope_v6_em(String key)
  {
    return (String)getValue(ipscope_v6_em,key);
  }

  public String getAuto_checkin() {
	  return (String)getRowValue(auto_checkin);
	}
  public String getAuto_checkin(String key) {
    return (String)getValue(auto_checkin,key);
  }

  public String getAuto_checkin_before() {
	  return (String)getRowValue(auto_checkin_before);
	}
  public String getAuto_checkin_before(String key) {
    return (String)getValue(auto_checkin_before,key);
  }

  public String getAuto_checkin_after() {
	  return (String)getRowValue(auto_checkin_after);
	}
  public String getAuto_checkin_after(String key) {
    return (String)getValue(auto_checkin_after,key);
  }

  public String getAuto_checkout() {
    return (String)getRowValue(auto_checkout);
  }
  public String getAuto_checkout(String key) {
    return (String)getValue(auto_checkout,key);
  }

  public String getAuto_checkout_before() {
    return (String)getRowValue(auto_checkout_before);
  }
  public String getAuto_checkout_before(String key) {
    return (String)getValue(auto_checkout_before,key);
  }

  public String getAuto_checkout_after() {
    return (String)getRowValue(auto_checkout_after);
  }
  public String getAuto_checkout_after(String key) {
    return (String)getValue(auto_checkout_after,key);
  }

  public String getCalmethod() {
    return (String)getRowValue(calmethod);
  }
  public String getCalmethod(String key) {
    return (String)getValue(calmethod,key);
  }
}