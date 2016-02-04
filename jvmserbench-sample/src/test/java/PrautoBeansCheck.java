public class PrautoBeansCheck {
	/*
	 * @Test public void packTest() {
	 * 
	 * ByteBuffer byteBuffer = ByteBuffer.allocateDirect(256 );
	 * 
	 * CreateOptions testOptions = new MyCreateOptions();
	 * 
	 * errLog("object toString: "+testOptions);
	 * 
	 * PackedPayload<CreateOptions> createOptionsClassPackedPayload
	 * =PackedPayload.create(CreateOptions.class);
	 * createOptionsClassPackedPayload.put(testOptions, byteBuffer); ByteBuffer
	 * flip = (ByteBuffer) byteBuffer.flip();
	 * 
	 * errLog("wire protocol serialized object: \n---\n"+StandardCharsets.UTF_8.
	 * decode(byteBuffer.duplicate())+"\n---");
	 * 
	 * ByteBuffer duplicate = (ByteBuffer) byteBuffer.duplicate(); int i =
	 * PackedPayload.readSize(duplicate);
	 * errLog("wire protocol object size(not counting size): "+i); byte b =
	 * duplicate.get();
	 * errLog("wire protocol true/notNull bitmap: "+toBinaryString(b & 0xff));
	 * 
	 * 
	 * CreateOptions createOptions =
	 * PackedPayload.create(CreateOptions.class).get(CreateOptions.class, flip);
	 * String cache = testOptions.getCache(); String cache1 = createOptions
	 * .getCache(); org.junit.Assert.assertEquals(cache1, cache,"someString"); }
	 * 
	 * private class MyCreateOptions implements CreateOptions { public boolean
	 * isAutoCompaction() { return true; }
	 * 
	 * @Override public String getName() { return "theName"; }
	 * 
	 * @Override public String getCache() { return "someString"; }
	 * 
	 * @Override public String getAdapter() { return "someOtherString"; }
	 * 
	 * @Override public String toString() { return
	 * getClass().getSimpleName()+":{" + "name:"+getName()+
	 * ",cache:"+getCache()+ ",adapter:"+getAdapter()+
	 * 
	 * 
	 * "} "; } }
	 */
}
