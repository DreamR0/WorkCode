import cn.net.drm.edi.client.DrmAgent;
import cn.net.drm.edi.exception.DrmException;
import cn.net.drm.edi.header.reader.DrmFileInfo;
import org.junit.Test;

import java.io.File;

public class encrpy {
    @Test
    public void testGetDrmFileInfo() throws DrmException {
        DrmFileInfo info = DrmAgent.getInstance().getDrmFileInfo(new File("D:\\1.docx"));
        System.out.println(info);
    }
}
