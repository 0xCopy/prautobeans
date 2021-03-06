import kouchdb.command.CreateOptions;
import org.junit.Test;
import prauto.io.ErrLog;
import prauto.io.PackedPayload;
import prauto.io.PackedPayloadUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static java.lang.Integer.toBinaryString;

public class PrautoBeansCheck
{
    @Test
    public void packTest() {

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(256 );

        CreateOptions testOptions = new MyCreateOptions();

        ErrLog.errLog("object toString: "+testOptions);

        PackedPayload<CreateOptions> createOptionsClassPackedPayload = PackedPayloadUtil.create(CreateOptions.class);
        createOptionsClassPackedPayload.put(testOptions, byteBuffer);
        ByteBuffer flip = (ByteBuffer) byteBuffer.flip();

        ErrLog.errLog("wire protocol serialized object: \n---\n"+StandardCharsets.UTF_8.decode(byteBuffer.duplicate())+"\n---");

        ByteBuffer duplicate = byteBuffer.duplicate();
        int i =  PackedPayloadUtil.readSize(duplicate);
        ErrLog.errLog("wire protocol object size(not counting size): "+i);
        byte b = duplicate.get();
        ErrLog.errLog("wire protocol true/notNull bitmap: "+toBinaryString(b & 0xff));


        CreateOptions createOptions = (CreateOptions) PackedPayloadUtil.create(CreateOptions.class).get(CreateOptions.class, flip);
        String cache = testOptions.getCache();
        String cache1 = createOptions .getCache();
        org.junit.Assert.assertEquals(cache1, cache,"someString");
    }

    private class MyCreateOptions implements CreateOptions {
        public boolean isAutoCompaction() {
            return true;
        }

        @Override
        public String getName() {
            return "theName";
        }

        @Override
        public String getCache() {
            return "someString";
        }

        @Override
        public String getAdapter() {
            return "someOtherString";
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()+":{" +
                    "name:"+getName()+
                    ",cache:"+getCache()+
                    ",adapter:"+getAdapter()+


                    "} ";
        }
    }
}
