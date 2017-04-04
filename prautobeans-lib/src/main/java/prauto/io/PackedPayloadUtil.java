package prauto.io;

import prauto.ann.ProtoNumber;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by jim on 17/04/05.
 */
public interface PackedPayloadUtil {


    Map<Class, Integer> VIEWSIZES = new HashMap<Class, Integer>() {{
        put(long.class, 8);
        put(double.class, 8);
        put(int.class, 4);
        put(float.class, 4);
        put(short.class, 2);
        put(byte.class, 1);
        put(boolean.class, 1);
    }};
    /**
     * lambdas that set values to a stream
     */

    Map<Class, BiConsumer<ByteBuffer, Object>> VIEWSETTER = new HashMap<Class, BiConsumer<ByteBuffer, Object>>() {{
        put(long.class, (byteBuffer, o) -> byteBuffer.putLong((long) o));
        put(double.class, (byteBuffer, o) -> byteBuffer.putDouble((double) o));
        put(int.class, (byteBuffer, o) -> byteBuffer.putInt((int) o));
        put(float.class, (byteBuffer, o) -> byteBuffer.putFloat((float) o));
        put(short.class, (byteBuffer, o) -> byteBuffer.putShort((short) o));
        put(byte.class, (byteBuffer, o) -> byteBuffer.put((byte) o));

        put(String.class, (byteBuffer, o) -> {
            byte[] o1 = o.toString().getBytes(UTF_8);
            int begin = byteBuffer.position();
            byteBuffer.position(begin + 5);
            byteBuffer.put(o1);
            writeSize(byteBuffer, begin, o1.length);
        });
        put(boolean.class, (byteBuffer, o) -> byteBuffer.put((byte) ((boolean) o ? 1 : 0)));
    }};
    /**
     * methods of ByteBuffer that grab from the stream
     */
    Map<Class, Function<ByteBuffer, Object>> VIEWGETTER = new HashMap<Class, Function<ByteBuffer, Object>>() {
        {
            put(long.class, ByteBuffer::getLong);
            put(double.class, ByteBuffer::getDouble);
            put(int.class, ByteBuffer::getInt);
            put(float.class, ByteBuffer::getFloat);
            put(short.class, ByteBuffer::getShort);
            put(byte.class, ByteBuffer::get);


            put(String.class, byteBuffer -> {
                int anInt = readSize(byteBuffer);
                byte[] bytes = new byte[anInt];
                byteBuffer.get(bytes);
                return new String(bytes, UTF_8);
            });
            put(boolean.class, byteBuffer -> 0 != byteBuffer.get());
        }
    };
    Comparator<Method> METHOD_COMPARATOR = Comparator.comparingInt(o -> o.getAnnotation(ProtoNumber.class).value());

    Map<Class, PackedPayload> CODEX = new HashMap<>();

    static <T, C extends Class<T>> PackedPayload create(C c) {
        return CODEX.computeIfAbsent(c, PackedPayload::new);
    }

    /**
     * given an output bytebuffer with 5 bytes headroom in front:
     * <p/>
     * if size is less than 255 we write a byte and move the page in place to have a 1 byte size
     * <p/>
     * otherwise we encode 0xff followed by the int32 size.
     *
     * @param out   bytebuffer with data starting at begin+5
     * @param begin the starting mark
     * @param size  the payload actual size used in the out buf.
     */
    static void writeSize(ByteBuffer out, int begin, long size) {
        ByteBuffer writeBuf = (ByteBuffer) out.duplicate().position(begin);
        if (255 > size) {
            writeBuf.put((byte) (size & 0xff));
            writeBuf.put((ByteBuffer) out.duplicate().flip().position(begin + 5));
            out.position(writeBuf.position());
        }
        else
            writeBuf.put((byte) 0xff).putInt((int) (size & 0xffff_ffffL));
    }

    static int readSize(ByteBuffer in) {
        long size = in.get() & 0xff;
        if (0xff == size) {
            size = in.getInt() & 0xffff_ffffL;
        }
        return (int) size;
    }
}
