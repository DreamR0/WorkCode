package com.customization.qc2430162.Util;

public class KqReportUtil {

    /**
     * 这里判断是否要给单元格标注背景色(如果有加班就标注背景色)
     * @param cellValue
     * @return
     */
    public Boolean setCellColor(String cellValue){
        boolean isSet = false;

        if(cellValue.contains("加班")){
            isSet = true;
        }
        return isSet;
    }
}
